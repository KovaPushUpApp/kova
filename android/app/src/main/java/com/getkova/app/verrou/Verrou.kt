package com.getkova.app.verrou

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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

/** L'écran de verrou, avec le nom et l'icône de l'app bloquée. Sert au rideau du service et à [VerrouActivity]. */
@Composable
private fun ContenuDuVerrou(
    contexte: Context,
    paquet: String?,
    jeu: EtatJeu,
    maintenant: Long,
    commencer: () -> Unit,
    utiliserLeStock: () -> Unit,
    fermer: () -> Unit,
    urgence: () -> Unit,
) {
    val app by produceState<AppInstallee?>(null, paquet) {
        value = paquet?.let { AppsInstallees.trouver(contexte, it) }
    }
    EcranVerrou(
        nomDeLApp = app?.nom ?: paquet?.let { AppsInstallees.nom(contexte, it) },
        icone = app?.icone,
        jeu = jeu,
        maintenant = maintenant,
        zone = ZoneId.systemDefault(),
        commencer = commencer,
        utiliserLeStock = utiliserLeStock,
        fermer = fermer,
        urgence = urgence,
    )
}

/** Ce qu'une vue Compose attend d'un écran, pour vivre dans une fenêtre posée par le service. */
private class ProprietaireDuRideau : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val cycle = LifecycleRegistry(this)
    private val sauvegarde = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = cycle
    override val savedStateRegistry: SavedStateRegistry get() = sauvegarde.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    fun demarrer() {
        sauvegarde.performRestore(null)
        cycle.currentState = Lifecycle.State.RESUMED
    }

    fun arreter() {
        cycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

/**
 * Le verrouillage réel des apps sur Android.
 *
 * Ce service d'accessibilité ne reçoit qu'une information : le nom du paquet de
 * l'app qui passe au premier plan. Il ne lit pas le contenu des écrans (voir
 * res/xml/service_verrou.xml) et n'enregistre rien. Si l'app est verrouillée,
 * il pose tout de suite l'écran de verrou par dessus, dans une fenêtre à lui (le
 * "rideau"), puis ouvre [VerrouActivity] qui prend le relais. Le rideau ne dépend
 * pas du bon vouloir du téléphone : certains retardent ou refusent l'ouverture d'un
 * écran par une app en arrière-plan, et l'app bloquée restait alors utilisable.
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

    /** Le rideau : l'écran de verrou posé par dessus l'app bloquée. */
    private var rideau: View? = null
    private var proprietaire: ProprietaireDuRideau? = null
    private var paquetDuRideau by mutableStateOf<String?>(null)
    private var jeuDuRideau by mutableStateOf(EtatJeu())
    private var instantDuRideau by mutableStateOf(0L)

    /** Vrai quand le rideau a dû renvoyer lui-même à l'accueil : il reste alors par dessus. */
    private var renvoyeALAccueil = false

    /** [VerrouActivity] n'est pas arrivée à temps : on ferme l'app bloquée, le rideau fait le verrou. */
    private val secours = Runnable {
        if (rideau != null) {
            renvoyeALAccueil = true
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }
    private val retirerLeRideauPlusTard = Runnable { retirerLeRideau() }

    private val paquetDeLAccueil: String? by lazy {
        packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
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
                // Un écran de Kova est devant : il remplace le rideau.
                retirerLeRideau()
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
                retirerLeRideau()
                suspendre()
            } else {
                controler(paquetAuPremierPlan)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        enService = this
        val filtre = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(Intent.ACTION_USER_PRESENT) }
        registerReceiver(ecran, filtre)
    }

    override fun onDestroy() {
        oublier()
        retirerLeRideau()
        if (enService === this) enService = null
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
            // Une autre app est devant : le rideau n'a plus rien à couvrir. Sauf sur
            // l'accueil où il a lui-même renvoyé : là, il reste jusqu'à ce qu'on réponde.
            if (!(renvoyeALAccueil && paquet == paquetDeLAccueil)) {
                retirerLeRideau()
            }
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
            // Le rideau d'abord : il couvre l'app à l'instant, quoi que fasse le téléphone ensuite.
            afficherLeRideau(paquet, etat, maintenant)
            // Réglage d'essai, version de test seulement : le rideau seul, comme sur un
            // téléphone qui refuse d'ouvrir l'écran de Kova.
            if (com.getkova.app.BuildConfig.DEBUG && getSharedPreferences("kova", MODE_PRIVATE).getBoolean("essaiRideauSeul", false)) return
            runCatching {
                startActivity(
                    Intent(this, VerrouActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        .putExtra(VerrouActivity.PAQUET, paquet),
                )
            }
        } else {
            retirerLeRideau()
            // L'app est ouverte pendant une fenêtre : on la reverrouille à la fin, même si Kova est fermée.
            val fin = etat.finDOuverture(maintenant, zone) ?: return
            finDeLaBulle = fin
            principal.postDelayed(montrerLaBulle, (fin - maintenant - DERNIERE_MINUTE).coerceAtLeast(0))
            principal.postDelayed(reverifier, (fin - maintenant).coerceAtLeast(0) + MARGE)
        }
    }

    /**
     * Pose l'écran de verrou par dessus l'app bloquée, en plein écran. Il capte les
     * appuis et le bouton retour : l'app dessous n'est plus utilisable.
     */
    private fun afficherLeRideau(paquet: String, etat: EtatJeu, maintenant: Long) {
        paquetDuRideau = paquet
        jeuDuRideau = etat
        instantDuRideau = maintenant
        principal.removeCallbacks(retirerLeRideauPlusTard)
        principal.removeCallbacks(secours)
        if (!renvoyeALAccueil) {
            principal.postDelayed(secours, DELAI_DE_SECOURS)
        }
        if (rideau != null) return

        val cycle = ProprietaireDuRideau()
        val contenu = ComposeView(this).apply {
            setContent {
                ContenuDuVerrou(
                    contexte = this@ServiceVerrou,
                    paquet = paquetDuRideau,
                    jeu = jeuDuRideau,
                    maintenant = instantDuRideau,
                    commencer = { ouvrirKovaDepuisLeRideau(MainActivity.ACTION_SEANCE) },
                    utiliserLeStock = { utiliserLeStockDepuisLeRideau() },
                    fermer = { fermerLeRideau() },
                    urgence = { ouvrirKovaDepuisLeRideau(MainActivity.ACTION_URGENCE) },
                )
            }
        }
        val boite = object : FrameLayout(this) {
            // Le bouton retour ne doit pas ramener dans l'app bloquée.
            override fun dispatchKeyEvent(evenement: KeyEvent): Boolean {
                if (evenement.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (evenement.action == KeyEvent.ACTION_UP) fermerLeRideau()
                    return true
                }
                return super.dispatchKeyEvent(evenement)
            }
        }.apply {
            // Opaque dès la première image, avant même que le contenu soit dessiné.
            setBackgroundColor(Tokens.Couleurs.fond.toArgb())
            setViewTreeLifecycleOwner(cycle)
            setViewTreeSavedStateRegistryOwner(cycle)
            setViewTreeViewModelStoreOwner(cycle)
            addView(contenu, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val place = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        cycle.demarrer()
        val ajoutee = runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).addView(boite, place) }.isSuccess
        if (!ajoutee) {
            cycle.arreter()
            principal.removeCallbacks(secours)
            return
        }
        rideau = boite
        proprietaire = cycle
    }

    /** Retire le rideau. Sans effet s'il n'est pas affiché. Appelé aussi par [VerrouActivity] quand elle arrive. */
    fun retirerLeRideau() {
        principal.removeCallbacks(secours)
        principal.removeCallbacks(retirerLeRideauPlusTard)
        renvoyeALAccueil = false
        val vue = rideau ?: return
        rideau = null
        runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(vue) }
        proprietaire?.arreter()
        proprietaire = null
    }

    /** "Plus tard" ou le bouton retour : on quitte l'app bloquée pour l'accueil du téléphone. */
    private fun fermerLeRideau() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        retirerLeRideau()
    }

    /** Ouvre Kova sur la séance ou l'urgence. Le rideau part quand l'écran de Kova est devant. */
    private fun ouvrirKovaDepuisLeRideau(action: String) {
        principal.removeCallbacks(secours)
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .setAction(action)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        // Filet : si l'arrivée de Kova n'est pas signalée, le rideau ne reste pas par dessus.
        principal.postDelayed(retirerLeRideauPlusTard, DELAI_DE_SECOURS)
    }

    /** Ouvre une fenêtre avec tout le temps en stock, puis rend l'app qui était bloquée. */
    private fun utiliserLeStockDepuisLeRideau() {
        val paquet = paquetDuRideau
        val maintenant = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val (etat, fin) = Depot.lire(this).mettreAJour(maintenant).ouvrirFenetre(maintenant, zone)
        if (fin == null) return
        Depot.ecrire(this, etat)
        Minuteur.mettreAJour(this, etat, maintenant, zone)
        retirerLeRideau()
        paquet?.let { packageManager.getLaunchIntentForPackage(it) }?.let { runCatching { startActivity(it) } }
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

        /** Délai laissé à [VerrouActivity] pour arriver avant que le rideau ferme lui-même l'app bloquée. */
        private const val DELAI_DE_SECOURS = 1_500L

        /** Le service en marche, pour que [VerrouActivity] retire le rideau en arrivant. */
        @Volatile
        var enService: ServiceVerrou? = null
            private set

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
        // Cet écran est devant : le rideau posé par le service a fini son travail.
        ServiceVerrou.enService?.retirerLeRideau()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        barresSombres()
        paquet = intent.getStringExtra(PAQUET)
        setContent {
            // Le bouton retour ne doit pas ramener dans l'app bloquée.
            BackHandler { quitterVersLAccueil() }
            ContenuDuVerrou(
                contexte = this,
                paquet = paquet,
                jeu = etatDuJeu,
                maintenant = instant,
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
        ServiceVerrou.enService?.retirerLeRideau()
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
