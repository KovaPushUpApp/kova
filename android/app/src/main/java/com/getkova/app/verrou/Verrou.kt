package com.getkova.app.verrou

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.getkova.app.R
import com.getkova.app.design.Tokens
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.getkova.app.jeu.EtatJeu
import com.getkova.app.Mesures
import com.getkova.app.jeu.fenetreOuverte
import com.getkova.app.jeu.ouvrirFenetre
import com.getkova.app.jeu.suspendreFenetre
import com.getkova.app.jeu.urgenceEnCours
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

    /** L'app correspondant à ce paquet, avec son nom et son icône. */
    suspend fun trouver(contexte: Context, paquet: String): AppInstallee? =
        charger(contexte).firstOrNull { it.paquet == paquet }

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

    /** Le minuteur de la dernière minute, posé par dessus l'app, et l'instant où il s'arrête. */
    private var bulle: View? = null
    private var texteDeLaBulle: TextView? = null
    private var finDeLaBulle = 0L
    private val montrerLaBulle = Runnable { afficherLaBulle() }
    private val battre = object : Runnable {
        override fun run() {
            val reste = finDeLaBulle - System.currentTimeMillis()
            if (reste <= 0 || bulle == null) {
                retirerLaBulle()
                return
            }
            // Arrondi vers le haut : on lit "1:00" puis "0:59", jamais "0:00" avant la fin.
            val secondes = ((reste + 999) / 1000).toInt()
            texteDeLaBulle?.text = getString(R.string.bulle_temps, secondes / 60, secondes % 60)
            principal.postDelayed(this, reste % 1000 + 20)
        }
    }

    override fun onAccessibilityEvent(evenement: AccessibilityEvent?) {
        if (evenement?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquet = evenement.packageName?.toString() ?: return
        // Le clavier et l'interface du système passent au premier plan sans qu'on change d'app.
        if (paquet == packageName) {
            // Un écran de Kova passe au premier plan : le minuteur n'a plus lieu d'être.
            // Le minuteur lui-même est une fenêtre de Kova, mais pas un écran : on l'ignore,
            // sinon il se retirerait tout seul et annulerait le reverrouillage.
            if (evenement.className?.toString().orEmpty().startsWith(packageName)) {
                oublier()
                suspendre()
            }
            return
        }
        // Le clavier, les boîtes du système et les fenêtres flottantes passent au premier
        // plan sans qu'on ait changé d'app : ils ne doivent rien annuler.
        if (paquet in paquetsIgnores || !estUneApp(paquet)) return
        paquetAuPremierPlan = paquet
        controler(paquet)
    }

    override fun onInterrupt() {}

    /** Écran éteint : le temps ne se décompte plus. Téléphone déverrouillé : on regarde où on est. */
    private val ecran = object : android.content.BroadcastReceiver() {
        override fun onReceive(contexte: Context, intention: Intent) {
            if (intention.action == Intent.ACTION_SCREEN_OFF) {
                oublier()
                suspendre()
            } else {
                controler(paquetAuPremierPlan)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filtre = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(Intent.ACTION_USER_PRESENT) }
        registerReceiver(ecran, filtre)
    }

    override fun onDestroy() {
        oublier()
        runCatching { unregisterReceiver(ecran) }
        suspendre()
        super.onDestroy()
    }

    /** On a quitté les apps verrouillées : le temps qui reste retourne au solde. */
    private fun suspendre() {
        val maintenant = System.currentTimeMillis()
        val etat = Depot.lire(this).mettreAJour(maintenant)
        if (!etat.fenetreOuverte(maintenant)) return
        val suspendu = etat.suspendreFenetre(maintenant)
        Depot.ecrire(this, suspendu)
        Minuteur.mettreAJour(this, suspendu, maintenant, ZoneId.systemDefault())
    }

    private val vraiesApps = mutableMapOf<String, Boolean>()

    /** Vrai pour une app qu'on ouvre depuis le lanceur, ou pour l'écran d'accueil du téléphone. */
    private fun estUneApp(paquet: String): Boolean = vraiesApps.getOrPut(paquet) {
        val accueil = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        packageManager.getLaunchIntentForPackage(paquet) != null || accueil?.activityInfo?.packageName == paquet
    }

    /** Annule ce qui était prévu et retire le minuteur. */
    private fun oublier() {
        principal.removeCallbacks(reverifier)
        principal.removeCallbacks(montrerLaBulle)
        retirerLaBulle()
    }

    /**
     * Bloque l'app si elle est verrouillée. Si elle est ouverte pendant une
     * fenêtre, prévoit le minuteur de la dernière minute, puis le reverrouillage.
     */
    private fun controler(paquet: String?) {
        oublier()
        paquet ?: return
        val maintenant = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        var etat = Depot.lire(this).mettreAJour(maintenant)
        if (paquet !in etat.apps && paquet !in etat.appsRetirees) {
            suspendre()
            return
        }
        // Une app verrouillée passe devant : le temps gagné se décompte à partir de maintenant.
        if (!etat.urgenceEnCours(maintenant) && !etat.fenetreOuverte(maintenant)) {
            val (ouvert, fin) = etat.ouvrirFenetre(maintenant, zone)
            if (fin != null) {
                etat = ouvert
                Depot.ecrire(this, etat)
                Minuteur.mettreAJour(this, etat, maintenant, zone)
            }
        }
        if (etat.estBloquee(paquet, maintenant, zone)) {
            Mesures.suivre(this, "verrou_affiche")
            startActivity(
                Intent(this, VerrouActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .putExtra(VerrouActivity.PAQUET, paquet),
            )
        } else {
            // L'app est ouverte pendant une fenêtre : on la reverrouille à la fin, même si Kova est fermée.
            val fin = etat.finDOuverture(maintenant, zone) ?: return
            finDeLaBulle = fin
            principal.postDelayed(montrerLaBulle, (fin - maintenant - DERNIERE_MINUTE).coerceAtLeast(0))
            principal.postDelayed(reverifier, (fin - maintenant).coerceAtLeast(0) + MARGE)
        }
    }

    /**
     * Pose le gros minuteur en haut de l'écran, par dessus l'app en cours. Il ne
     * capte aucun appui : on continue d'utiliser l'app normalement dessous.
     */
    private fun afficherLaBulle() {
        if (bulle != null || finDeLaBulle <= System.currentTimeMillis()) return
        val densite = resources.displayMetrics.density
        fun dp(valeur: Float) = (valeur * densite).toInt()
        val sombre = Tokens.Couleurs.texteSurAccent.toArgb()

        val titre = TextView(this).apply {
            text = getString(R.string.bulle_titre)
            setTextColor(sombre)
            textSize = Tokens.TaillesTexte.legende
            typeface = ResourcesCompat.getFont(this@ServiceVerrou, R.font.inter_semibold)
            gravity = Gravity.CENTER
        }
        val temps = TextView(this).apply {
            setTextColor(sombre)
            textSize = Tokens.TaillesTexte.chiffre
            typeface = ResourcesCompat.getFont(this@ServiceVerrou, R.font.space_grotesk_bold)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        val boite = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Tokens.Couleurs.accent.toArgb())
                cornerRadius = Tokens.Arrondis.grand * densite
            }
            setPadding(dp(Tokens.Espacements.e34), dp(Tokens.Espacements.e13), dp(Tokens.Espacements.e34), dp(Tokens.Espacements.e13))
            elevation = Tokens.Espacements.e8 * densite
            addView(titre)
            addView(temps)
        }
        val place = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(Tokens.Espacements.e55)
        }
        val ajoutee = runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).addView(boite, place) }.isSuccess
        if (!ajoutee) return
        bulle = boite
        texteDeLaBulle = temps
        battre.run()
    }

    private fun retirerLaBulle() {
        principal.removeCallbacks(battre)
        bulle?.let { vue -> runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(vue) } }
        bulle = null
        texteDeLaBulle = null
    }

    companion object {
        private const val MARGE = 500L

        /** Le minuteur s'affiche quand il reste une minute. */
        private const val DERNIERE_MINUTE = 60_000L
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
    /** Paquet de l'app bloquée affichée. */
    private var paquet by mutableStateOf<String?>(null)

    /** L'état du jeu et l'heure, relus à chaque affichage : cet écran est réutilisé d'un blocage à l'autre. */
    private var etatDuJeu by mutableStateOf(EtatJeu())
    private var instant by mutableStateOf(0L)

    override fun onResume() {
        super.onResume()
        instant = System.currentTimeMillis()
        etatDuJeu = Depot.lire(this).mettreAJour(instant)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        barresSombres()
        paquet = intent.getStringExtra(PAQUET)
        setContent {
            // Le bouton retour ne doit pas ramener dans l'app bloquée.
            BackHandler { quitterVersLAccueil() }
            val bloque = paquet
            val maintenant = instant
            val jeu = etatDuJeu
            val app by produceState<AppInstallee?>(null, bloque) {
                value = bloque?.let { AppsInstallees.trouver(this@VerrouActivity, it) }
            }
            EcranVerrou(
                nomDeLApp = app?.nom ?: bloque?.let { AppsInstallees.nom(this, it) },
                icone = app?.icone,
                jeu = jeu,
                maintenant = maintenant,
                zone = ZoneId.systemDefault(),
                commencer = { ouvrirKova(MainActivity.ACTION_SEANCE) },
                utiliserLeStock = { utiliserLeStock() },
                fermer = { quitterVersLAccueil() },
                urgence = { ouvrirKova(MainActivity.ACTION_URGENCE) },
            )
        }
    }

    /** Une autre app bloquée vient de s'ouvrir : l'écran change d'app. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        paquet = intent.getStringExtra(PAQUET)
    }

    /** Ouvre une fenêtre avec tout le temps en stock, puis retourne dans l'app qui était bloquée. */
    private fun utiliserLeStock() {
        val maintenant = System.currentTimeMillis()
        val (etat, fin) = Depot.lire(this).mettreAJour(maintenant).ouvrirFenetre(maintenant, ZoneId.systemDefault())
        if (fin == null) return
        Depot.ecrire(this, etat)
        Minuteur.mettreAJour(this, etat, maintenant, ZoneId.systemDefault())
        paquet?.let { packageManager.getLaunchIntentForPackage(it) }?.let { startActivity(it) }
        finish()
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
