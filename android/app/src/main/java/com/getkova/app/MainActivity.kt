package com.getkova.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.getkova.app.comptage.CameraSeance
import com.getkova.app.design.Icone
import com.getkova.app.design.Tokens
import com.getkova.app.design.t
import com.getkova.app.ecrans.BarreOnglets
import com.getkova.app.ecrans.Destination
import com.getkova.app.ecrans.EcranAbonnement
import com.getkova.app.ecrans.EcranAccueil
import com.getkova.app.ecrans.EcranApps
import com.getkova.app.ecrans.EcranAppsReglage
import com.getkova.app.ecrans.EcranAutorisation
import com.getkova.app.ecrans.EcranBienvenue
import com.getkova.app.ecrans.EcranChantier
import com.getkova.app.ecrans.EcranChoc
import com.getkova.app.ecrans.EcranCompteur
import com.getkova.app.ecrans.EcranEditeurPlage
import com.getkova.app.ecrans.EcranEssai
import com.getkova.app.ecrans.EcranExercice
import com.getkova.app.ecrans.EcranGagne
import com.getkova.app.ecrans.EcranLimiteGratuite
import com.getkova.app.ecrans.EcranObjectif
import com.getkova.app.ecrans.EcranOffre
import com.getkova.app.ecrans.EcranPaywall
import com.getkova.app.ecrans.EcranPlacement
import com.getkova.app.ecrans.EcranPlanning
import com.getkova.app.ecrans.EcranRegle
import com.getkova.app.ecrans.EcranReglages
import com.getkova.app.ecrans.EcranReserve
import com.getkova.app.ecrans.EcranStats
import com.getkova.app.ecrans.EcranTemps
import com.getkova.app.ecrans.EcranUrgence
import com.getkova.app.ecrans.EcranVerrou
import com.getkova.app.ecrans.Onglet
import com.getkova.app.ecrans.ouvrirFicheDeLApp
import com.getkova.app.verrou.Minuteur
import com.getkova.app.verrou.ServiceVerrou

class MainActivity : ComponentActivity() {
    private val etat: EtatApp by viewModels()

    /** Action demandée depuis l'écran de verrou d'une app bloquée. */
    private var actionDemandee by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        barresSombres()
        actionDemandee = intent?.action
        if (BuildConfig.DEBUG && intent?.hasExtra(EXTRA_COMPTAGE_SIMULE) == true) {
            // Version de test seulement : permet d'essayer le parcours sans caméra.
            etat.definirOption("comptageSimule", intent.getBooleanExtra(EXTRA_COMPTAGE_SIMULE, false))
        }
        if (BuildConfig.DEBUG && intent?.hasExtra(EXTRA_FENETRE_SECONDES) == true) {
            // Version de test seulement : ouvre une fenêtre très courte, pour vérifier le reverrouillage.
            etat.ouvrirFenetreDeTest(intent.getIntExtra(EXTRA_FENETRE_SECONDES, 0))
        }
        setContent {
            Box(Modifier.fillMaxSize().background(Tokens.Couleurs.fond)) {
                if (etat.onboardingTermine) {
                    AppPrincipale(etat, actionDemandee) { actionDemandee = null }
                } else {
                    ParcoursOnboarding(etat)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        actionDemandee = intent.action
    }

    companion object {
        const val ACTION_SEANCE = "com.getkova.app.SEANCE"
        const val ACTION_URGENCE = "com.getkova.app.URGENCE"
        private const val EXTRA_COMPTAGE_SIMULE = "comptageSimule"
        private const val EXTRA_FENETRE_SECONDES = "fenetreSecondes"
    }
}

/** Kova est toujours sombre : les icônes des barres du système restent claires. */
fun ComponentActivity.barresSombres() {
    val style = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}

/**
 * Les écrans de l'onboarding. Écart avec l'ordre du brief (section 7) : l'accès
 * de verrouillage passe avant le choix des apps, comme sur iPhone.
 */
private enum class Etape {
    BIENVENUE, TEMPS, CHOC, OBJECTIF, AUTO_ECRAN, APPS, REGLE, AUTO_CAMERA, ESSAI, PAYWALL, OFFRE, LIMITE,
}

@Composable
private fun ParcoursOnboarding(etat: EtatApp) {
    val contexte = LocalContext.current
    var chemin by rememberSaveable { mutableStateOf(listOf(Etape.BIENVENUE)) }
    val etape = chemin.last()
    val camera = remember { CameraSeance(contexte) }
    var serviceActif by remember { mutableStateOf(ServiceVerrou.actif(contexte)) }
    var serviceDemande by rememberSaveable { mutableStateOf(false) }
    var cameraRefusee by rememberSaveable { mutableStateOf(false) }
    var messageAchat by remember { mutableStateOf(false) }

    fun aller(suivante: Etape) {
        if (chemin.last() != suivante) chemin = chemin + suivante
    }
    fun reculer() {
        if (chemin.size > 1) chemin = chemin.dropLast(1)
    }
    fun terminer() {
        // Sans abonnement, une seule app peut rester verrouillée (brief, section 4).
        if (!etat.abonne && etat.jeu.apps.size > 1) aller(Etape.LIMITE) else etat.terminerOnboarding()
    }

    // Version de test : toucher le bouton du paywall donne tout de suite accès à tout.
    // Version publiée : le vrai achat Google Play, pas encore branché.
    fun acheter() {
        if (BuildConfig.DEBUG) {
            etat.definirOption("abonnementSimule", true)
            etat.terminerOnboarding()
        } else {
            messageAchat = true
        }
    }

    BackHandler(enabled = chemin.size > 1) { reculer() }
    // Au retour des réglages du téléphone, on regarde si le service a été activé.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceActif = ServiceVerrou.actif(contexte)
        if (serviceActif && chemin.last() == Etape.AUTO_ECRAN && serviceDemande) aller(Etape.APPS)
    }
    val demandeCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        cameraRefusee = !accordee
        if (accordee) aller(Etape.ESSAI)
    }

    when (etape) {
        Etape.BIENVENUE -> EcranBienvenue { aller(Etape.TEMPS) }
        Etape.TEMPS -> EcranTemps(etat, ::reculer) { aller(Etape.CHOC) }
        Etape.CHOC -> EcranChoc(etat, ::reculer) { aller(Etape.OBJECTIF) }
        Etape.OBJECTIF -> EcranObjectif(etat, ::reculer) { aller(Etape.AUTO_ECRAN) }
        Etape.AUTO_ECRAN -> EcranAutorisation(
            etape = 4,
            icone = Icone.CADENAS,
            titre = t(R.string.auto_ecran_titre),
            texte = t(R.string.auto_ecran_texte),
            arguments = listOf(t(R.string.auto_ecran_argument_1), t(R.string.auto_ecran_argument_2), t(R.string.auto_ecran_argument_3)),
            bouton = t(if (serviceDemande) R.string.auto_ecran_verifier else R.string.auto_ecran_bouton),
            retour = ::reculer,
            autoriser = {
                when {
                    ServiceVerrou.actif(contexte) -> aller(Etape.APPS)
                    else -> {
                        serviceDemande = true
                        ServiceVerrou.ouvrirLesReglages(contexte)
                    }
                }
            },
            // On ne peut pas continuer sans cet accès (brief, section 7).
            refus = if (serviceDemande && !serviceActif) t(R.string.auto_ecran_refus) else null,
            aide = if (serviceDemande && !serviceActif) t(R.string.auto_ecran_aide) else null,
            boutonSecondaire = if (serviceDemande && !serviceActif) t(R.string.auto_ecran_fiche) else null,
            actionSecondaire = { ouvrirFicheDeLApp(contexte) },
        )
        Etape.APPS -> EcranApps(etat, ::reculer) { aller(Etape.REGLE) }
        Etape.REGLE -> EcranRegle(etat, onboarding = true, retour = ::reculer) { aller(Etape.AUTO_CAMERA) }
        Etape.AUTO_CAMERA -> EcranAutorisation(
            etape = 7,
            icone = Icone.CAMERA,
            titre = t(R.string.auto_camera_titre),
            texte = t(R.string.auto_camera_texte),
            arguments = listOf(t(R.string.auto_camera_argument_1), t(R.string.auto_camera_argument_2), t(R.string.auto_camera_argument_3)),
            bouton = t(if (cameraRefusee) R.string.autorisation_reessayer else R.string.auto_camera_bouton),
            retour = ::reculer,
            autoriser = {
                if (camera.autorisee || etat.comptageSimule) aller(Etape.ESSAI) else demandeCamera.launch(Manifest.permission.CAMERA)
            },
            refus = if (cameraRefusee) t(R.string.auto_camera_refus) else null,
            boutonSecondaire = if (cameraRefusee) t(R.string.camera_ouvrir_reglages) else null,
            actionSecondaire = { ouvrirFicheDeLApp(contexte) },
        )
        Etape.ESSAI -> EcranEssai(etat, camera, ::reculer) { aller(Etape.PAYWALL) }
        Etape.PAYWALL -> EcranPaywall(
            etat,
            fermer = {
                // Fermer le paywall sans payer montre l'offre de rattrapage, une seule fois.
                if (etat.offreDejaVue) {
                    terminer()
                } else {
                    etat.definirOption("offreDejaVue", true)
                    aller(Etape.OFFRE)
                }
            },
            acheter = { acheter() },
            restaurer = { messageAchat = true },
        )
        Etape.OFFRE -> EcranOffre(fermer = ::terminer, prendre = ::acheter)
        Etape.LIMITE -> EcranLimiteGratuite(etat, voirAbonnement = { aller(Etape.PAYWALL) }, terminer = ::terminer)
    }
    if (messageAchat) {
        MessageAchat { messageAchat = false }
    }
}

/** Les achats Google Play ne sont pas encore branchés : le message le dit clairement. */
@Composable
private fun MessageAchat(fermer: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = fermer,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = fermer) {
                androidx.compose.material3.Text(t(R.string.commun_ok), color = Tokens.Couleurs.accent)
            }
        },
        title = { androidx.compose.material3.Text(t(R.string.achat_titre), color = Tokens.Couleurs.texte) },
        text = { androidx.compose.material3.Text(t(R.string.achat_indisponible), color = Tokens.Couleurs.texteSecondaire) },
        containerColor = Tokens.Couleurs.carte,
    )
}

/** L'app après l'onboarding : quatre onglets, et les écrans poussés par dessus. */
@Composable
private fun AppPrincipale(etat: EtatApp, actionDemandee: String?, actionTraitee: () -> Unit) {
    val contexte = LocalContext.current
    var onglet by rememberSaveable { mutableStateOf(Onglet.ACCUEIL) }
    var chemin by rememberSaveable { mutableStateOf(listOf<Destination>()) }
    var paywall by rememberSaveable { mutableStateOf(false) }
    var messageAchat by remember { mutableStateOf(false) }
    val camera = remember { CameraSeance(contexte) }
    var serviceActif by remember { mutableStateOf(ServiceVerrou.actif(contexte)) }

    fun aller(destination: Destination) {
        if (chemin.lastOrNull() != destination) chemin = chemin + destination
    }
    fun reculer() {
        chemin = chemin.dropLast(1)
    }
    fun revenirAuxOnglets() {
        chemin = emptyList()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        etat.rafraichir()
        serviceActif = ServiceVerrou.actif(contexte)
    }
    // L'utilisateur arrive de l'écran de verrou d'une app bloquée.
    LaunchedEffect(actionDemandee) {
        when (actionDemandee) {
            MainActivity.ACTION_SEANCE -> {
                paywall = false
                onglet = Onglet.ACCUEIL
                chemin = listOf(Destination.EXERCICE)
            }
            MainActivity.ACTION_URGENCE -> {
                paywall = false
                onglet = Onglet.ACCUEIL
                chemin = listOf(Destination.URGENCE)
            }
        }
        if (actionDemandee != null) actionTraitee()
    }
    // La caméra s'éteint dès qu'on quitte le placement et le compteur.
    LaunchedEffect(chemin) {
        if (Destination.PLACEMENT !in chemin && Destination.COMPTEUR !in chemin) camera.arreter()
    }
    val demandeCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        aller(Destination.PLACEMENT)
    }
    // La notification du temps restant demande une autorisation sur Android 13 et plus.
    // On la demande une seule fois, à la première entrée dans l'app.
    val demandeNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        etat.rafraichir()
    }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            !Minuteur.autorise(contexte) && !etat.notificationsDemandees
        ) {
            etat.definirOption("notificationsDemandees", true)
            demandeNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    BackHandler(enabled = paywall || chemin.isNotEmpty()) {
        when {
            paywall -> paywall = false
            // Quitter la séance ou la récompense ramène aux onglets, jamais au compteur.
            chemin.last() == Destination.COMPTEUR || chemin.last() == Destination.GAGNE -> revenirAuxOnglets()
            else -> reculer()
        }
    }

    if (paywall) {
        EcranPaywall(
            etat,
            fermer = { paywall = false },
            acheter = {
                // Version de test : accès immédiat. Version publiée : achat Google Play, pas encore branché.
                if (BuildConfig.DEBUG) {
                    etat.definirOption("abonnementSimule", true)
                    paywall = false
                } else {
                    messageAchat = true
                }
            },
            restaurer = { messageAchat = true },
        )
    } else when (chemin.lastOrNull()) {
        null -> Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (onglet) {
                    Onglet.ACCUEIL -> if (serviceActif) {
                        EcranAccueil(etat, { aller(Destination.EXERCICE) }, { aller(Destination.VERROU) })
                    } else {
                        // Le service a été coupé dans les réglages : sans lui, rien n'est verrouillé.
                        EcranReserve(t(R.string.auto_ecran_titre), t(R.string.auto_ecran_refus)) {
                            ServiceVerrou.ouvrirLesReglages(contexte)
                        }
                    }
                    Onglet.STATS -> if (etat.abonne) {
                        EcranStats(etat)
                    } else {
                        EcranReserve(t(R.string.stats_titre), t(R.string.reserve_stats)) { paywall = true }
                    }
                    Onglet.PLANNING -> EcranPlanning(etat, { paywall = true }) { aller(Destination.EDITEUR_PLAGE) }
                    Onglet.REGLAGES -> EcranReglages(etat, ::aller)
                }
            }
            BarreOnglets(
                onglet, { onglet = it },
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = Tokens.Ecran.margeCotes.dp)
                    .padding(bottom = Tokens.Espacements.e8.dp),
            )
        }
        Destination.VERROU -> EcranVerrou(
            nomDeLApp = null,
            icone = null,
            jeu = etat.jeu,
            maintenant = remember { System.currentTimeMillis() },
            zone = etat.zone,
            commencer = { chemin = listOf(Destination.EXERCICE) },
            utiliserLeStock = {
                etat.ouvrirFenetre()
                revenirAuxOnglets()
            },
            fermer = ::reculer,
            urgence = { aller(Destination.URGENCE) },
        )
        Destination.EXERCICE -> EcranExercice(
            etat, parDefaut = false, retour = ::reculer,
            valider = {
                if (camera.autorisee || etat.comptageSimule) aller(Destination.PLACEMENT) else demandeCamera.launch(Manifest.permission.CAMERA)
            },
            ouvrirPaywall = { paywall = true },
        )
        Destination.PLACEMENT -> EcranPlacement(etat, camera, ::reculer) { aller(Destination.COMPTEUR) }
        Destination.COMPTEUR -> EcranCompteur(etat, camera, abandonner = ::revenirAuxOnglets) {
            // Les minutes vont au solde dès la validation (brief, règle 3).
            etat.validerSeance()
            aller(Destination.GAGNE)
        }
        Destination.GAGNE -> EcranGagne(
            etat,
            ouvrir = {
                etat.ouvrirFenetre()
                revenirAuxOnglets()
            },
            garder = ::revenirAuxOnglets,
        )
        Destination.URGENCE -> EcranUrgence(
            etat, ::reculer,
            debloquer = {
                etat.utiliserUrgence()
                revenirAuxOnglets()
            },
            faireSesPompes = { chemin = listOf(Destination.EXERCICE) },
        )
        Destination.APPS_REGLAGE -> EcranAppsReglage(etat, ::reculer) { paywall = true }
        Destination.REGLE -> EcranRegle(etat, onboarding = false, retour = ::reculer, valider = ::reculer)
        Destination.EXERCICE_PAR_DEFAUT -> EcranExercice(etat, parDefaut = true, retour = ::reculer, valider = ::reculer) { paywall = true }
        Destination.ABONNEMENT -> EcranAbonnement(etat, ::reculer, { paywall = true }) { messageAchat = true }
        Destination.EDITEUR_PLAGE -> EcranEditeurPlage(annuler = ::reculer) { plage ->
            etat.ajouterPlage(plage)
            reculer()
        }
        Destination.CHANTIER -> EcranChantier(etat, ::reculer)
    }
    if (messageAchat) {
        MessageAchat { messageAchat = false }
    }
}
