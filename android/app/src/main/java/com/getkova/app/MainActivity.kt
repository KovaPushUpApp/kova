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
import com.getkova.app.ecrans.REPETITIONS_ESSAI
import com.getkova.app.ecrans.EcranChantier
import com.getkova.app.ecrans.EcranChoc
import com.getkova.app.ecrans.EcranCompteur
import com.getkova.app.ecrans.EcranMesures
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
import com.getkova.app.verrou.Rappel
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
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("rappel", false) == true) {
            // Version de test seulement : affiche le rappel quotidien sans attendre 18 h.
            com.getkova.app.verrou.Rappel.afficherSiUtile(this)
        }
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("finEssai", false) == true) {
            // Version de test seulement : affiche le rappel de fin d'essai sans attendre deux jours.
            Rappel.afficherFinEssai(this)
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

    override fun onStop() {
        super.onStop()
        // L'app passe au second plan : on envoie les statistiques en attente, si l'utilisateur a dit oui.
        Mesures.vider(this)
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

    /** La question des statistiques anonymes. Posée une fois, juste après l'accueil. */
    MESURES,
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
    var essaiReussi by rememberSaveable { mutableStateOf(false) }
    var messageAchat by remember { mutableStateOf(false) }

    LaunchedEffect(etape) { Mesures.suivre(contexte, "onboarding_etape_vue", "etape" to etape.name, "rang" to etape.ordinal) }

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
    fun acheter(essai: Boolean) {
        Mesures.suivre(contexte, "achat_demande", "origine" to etape.name, "essai" to essai)
        if (BuildConfig.DEBUG) {
            etat.definirOption("abonnementSimule", true)
            // Promis sur le paywall : un rappel la veille de la fin de l'essai.
            if (essai) Rappel.programmerFinEssai(contexte)
            etat.terminerOnboarding()
        } else {
            messageAchat = true
        }
    }

    BackHandler(enabled = chemin.size > 1) { reculer() }
    // Au retour des réglages du téléphone, on regarde si le service a été activé.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceActif = ServiceVerrou.actif(contexte)
        if (chemin.last() == Etape.AUTO_ECRAN && serviceDemande) {
            Mesures.suivre(contexte, "autorisation_resultat", "type" to "verrouillage", "accordee" to serviceActif)
        }
        if (serviceActif && chemin.last() == Etape.AUTO_ECRAN && serviceDemande) aller(Etape.APPS)
    }
    val demandeCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        cameraRefusee = !accordee
        Mesures.suivre(contexte, "autorisation_resultat", "type" to "camera", "accordee" to accordee)
        if (accordee) aller(Etape.ESSAI)
    }

    when (etape) {
        Etape.BIENVENUE -> EcranBienvenue {
            // La question n'est posée que si l'app peut envoyer quelque chose, et une seule fois.
            aller(if (Mesures.disponible && Mesures.consentement(contexte) == null) Etape.MESURES else Etape.TEMPS)
        }
        Etape.MESURES -> EcranMesures { acceptees ->
            etat.definirMesures(acceptees)
            // La question ne reste pas dans le chemin : "retour" ne la repose pas.
            chemin = chemin.dropLast(1) + Etape.TEMPS
        }
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
        Etape.ESSAI -> EcranEssai(etat, camera, ::reculer) {
            // L'essai est facultatif : on ne félicite que s'il a vraiment été fait.
            essaiReussi = camera.repetitions >= REPETITIONS_ESSAI
            Mesures.suivre(contexte, "essai_termine", "repetitions" to camera.repetitions, "reussi" to essaiReussi)
            aller(Etape.PAYWALL)
        }
        Etape.PAYWALL -> EcranPaywall(
            etat,
            essaiReussi = essaiReussi,
            fermer = {
                Mesures.suivre(contexte, "paywall_ferme", "origine" to "onboarding")
                // Fermer le paywall sans payer montre l'offre de rattrapage, une seule fois.
                if (etat.offreDejaVue) {
                    terminer()
                } else {
                    etat.definirOption("offreDejaVue", true)
                    aller(Etape.OFFRE)
                }
            },
            acheter = { annuel -> acheter(essai = annuel) },
            restaurer = { messageAchat = true },
        )
        Etape.OFFRE -> EcranOffre(
            fermer = {
                Mesures.suivre(contexte, "offre_refusee")
                terminer()
            },
            // L'offre de rattrapage n'a pas d'essai gratuit.
            prendre = { acheter(essai = false) },
        )
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
    val ecran = if (paywall) "PAYWALL" else chemin.lastOrNull()?.name ?: onglet.name
    LaunchedEffect(ecran) { Mesures.suivre(contexte, "ecran_vu", "ecran" to ecran, "abonne" to etat.abonne) }

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
    // Le rappel quotidien est activé d'office dès que les notifications sont permises.
    // Ensuite c'est le réglage de l'utilisateur qui décide : on n'y touche plus.
    fun proposerLeRappel() {
        if (Minuteur.autorise(contexte) && !Depot.reglages(contexte).contains(Rappel.CLE)) {
            etat.definirOption(Rappel.CLE, true)
            Rappel.programmer(contexte, true)
        }
    }
    // La notification du temps restant demande une autorisation sur Android 13 et plus.
    // On la demande une seule fois, à la première entrée dans l'app.
    val demandeNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accordee ->
        Mesures.suivre(contexte, "autorisation_resultat", "type" to "notifications", "accordee" to accordee)
        proposerLeRappel()
        etat.rafraichir()
    }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            !Minuteur.autorise(contexte) && !etat.notificationsDemandees
        ) {
            etat.definirOption("notificationsDemandees", true)
            demandeNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            proposerLeRappel()
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
            fermer = {
                Mesures.suivre(contexte, "paywall_ferme", "origine" to "app")
                paywall = false
            },
            acheter = { annuel ->
                Mesures.suivre(contexte, "achat_demande", "origine" to "app", "essai" to annuel)
                // Version de test : accès immédiat. Version publiée : achat Google Play, pas encore branché.
                if (BuildConfig.DEBUG) {
                    etat.definirOption("abonnementSimule", true)
                    if (annuel) Rappel.programmerFinEssai(contexte)
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
                        EcranReserve(t(R.string.auto_ecran_titre), t(R.string.auto_ecran_refus), t(R.string.auto_ecran_bouton)) {
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
        Destination.COMPTEUR -> EcranCompteur(
            etat, camera,
            abandonner = {
                Mesures.suivre(
                    contexte, "seance_abandonnee",
                    "repetitions" to camera.repetitions, "serie_demandee" to etat.repetitionsDeLaSeance,
                )
                revenirAuxOnglets()
            },
        ) { repetitions ->
            // La séance ne s'arrête plus seule : la validation éteint la caméra.
            camera.arreter()
            // Les minutes vont au solde dès la validation (brief, règle 3).
            etat.validerSeance(repetitions)
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
