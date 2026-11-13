package com.getkova.app.verrou

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.getkova.app.Depot
import com.getkova.app.MainActivity
import com.getkova.app.barresSombres
import com.getkova.app.ecrans.EcranVerrou
import com.getkova.app.jeu.estBloquee
import com.getkova.app.jeu.finDOuverture
import com.getkova.app.jeu.mettreAJour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** Une app du téléphone, telle qu'elle apparaît dans la liste à verrouiller. */
class AppInstallee(val paquet: String, val nom: String, val icone: ImageBitmap?)

/**
 * La liste des apps qui ont une icône dans le lanceur. Kova ne demande pas le
 * droit de lister tous les paquets : la déclaration `queries` du manifeste suffit.
 */
object AppsInstallees {
    private const val TAILLE_ICONE = 96
    private var cache: List<AppInstallee>? = null

    suspend fun charger(contexte: Context): List<AppInstallee> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val gestionnaire = contexte.packageManager
            val lanceur = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = gestionnaire.queryIntentActivities(lanceur, 0)
                .map { it.activityInfo }
                .filter { it.packageName != contexte.packageName }
                .distinctBy { it.packageName }
                .map { info ->
                    val icone = runCatching {
                        val dessin = info.loadIcon(gestionnaire)
                        val image = Bitmap.createBitmap(TAILLE_ICONE, TAILLE_ICONE, Bitmap.Config.ARGB_8888)
                        dessin.setBounds(0, 0, TAILLE_ICONE, TAILLE_ICONE)
                        dessin.draw(Canvas(image))
                        image.asImageBitmap()
                    }.getOrNull()
                    AppInstallee(info.packageName, info.loadLabel(gestionnaire).toString(), icone)
                }
                .sortedBy { it.nom.lowercase() }
            cache = apps
            apps
        }
    }

    fun nom(contexte: Context, paquet: String): String? = runCatching {
        val gestionnaire = contexte.packageManager
        gestionnaire.getApplicationLabel(gestionnaire.getApplicationInfo(paquet, 0)).toString()
    }.getOrNull()
}

/**
 * Le verrouillage réel des apps sur Android.
 *
 * Ce service d'accessibilité ne reçoit qu'une information : le nom du paquet de
 * l'app qui passe au premier plan. Il ne lit pas le contenu des écrans (voir
 * res/xml/service_verrou.xml) et n'enregistre rien. Si l'app est verrouillée,
 * il affiche l'écran de verrou de Kova par dessus.
 *
 * Tant que le service est actif, il tourne même quand Kova est fermée : c'est
 * lui qui reverrouille à la fin d'une fenêtre.
 */
class ServiceVerrou : AccessibilityService() {
    private val principal = Handler(Looper.getMainLooper())
    private var paquetAuPremierPlan: String? = null
    private val reverifier = Runnable { controler(paquetAuPremierPlan) }

    override fun onAccessibilityEvent(evenement: AccessibilityEvent?) {
        if (evenement?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquet = evenement.packageName?.toString() ?: return
        // Le clavier et l'interface du système passent au premier plan sans qu'on change d'app.
        if (paquet == packageName || paquet in paquetsIgnores) return
        paquetAuPremierPlan = paquet
        controler(paquet)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        principal.removeCallbacks(reverifier)
        super.onDestroy()
    }

    /** Bloque l'app si elle est verrouillée. Sinon, prévoit de revérifier à la fin de la fenêtre. */
    private fun controler(paquet: String?) {
        principal.removeCallbacks(reverifier)
        paquet ?: return
        val maintenant = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val etat = Depot.lire(this).mettreAJour(maintenant)
        if (paquet !in etat.apps && paquet !in etat.appsRetirees) return
        if (etat.estBloquee(paquet, maintenant, zone)) {
            startActivity(
                Intent(this, VerrouActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .putExtra(VerrouActivity.PAQUET, paquet),
            )
        } else {
            // L'app est ouverte pendant une fenêtre : on la reverrouille à la fin, même si Kova est fermée.
            val fin = etat.finDOuverture(maintenant, zone) ?: return
            principal.postDelayed(reverifier, (fin - maintenant).coerceAtLeast(0) + MARGE)
        }
    }

    companion object {
        private const val MARGE = 500L
        private val paquetsIgnores = setOf("com.android.systemui", "android")

        /** Vrai si l'utilisateur a activé le service dans les réglages d'accessibilité. */
        fun actif(contexte: Context): Boolean {
            val attendu = ComponentName(contexte, ServiceVerrou::class.java)
            val actifs = Settings.Secure.getString(contexte.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            return actifs.split(':').any { ComponentName.unflattenFromString(it) == attendu }
        }

        fun ouvrirLesReglages(contexte: Context) {
            runCatching {
                contexte.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

/** L'écran de verrou, affiché par dessus une app bloquée. Il reprend la maquette "Verrou". */
class VerrouActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        barresSombres()
        val paquet = intent.getStringExtra(PAQUET)
        val regle = Depot.lire(this).mettreAJour(System.currentTimeMillis()).regle
        setContent {
            // Le bouton retour ne doit pas ramener dans l'app bloquée.
            BackHandler { quitterVersLAccueil() }
            EcranVerrou(
                nomDeLApp = paquet?.let { AppsInstallees.nom(this, it) },
                repetitions = regle.repetitions,
                minutes = regle.minutes,
                commencer = { ouvrirKova(MainActivity.ACTION_SEANCE) },
                fermer = { quitterVersLAccueil() },
                urgence = { ouvrirKova(MainActivity.ACTION_URGENCE) },
            )
        }
    }

    private fun ouvrirKova(action: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    /** Ferme l'app bloquée en revenant à l'écran d'accueil du téléphone. */
    private fun quitterVersLAccueil() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val PAQUET = "paquet"
    }
}
