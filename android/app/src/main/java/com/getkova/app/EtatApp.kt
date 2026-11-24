package com.getkova.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.getkova.app.jeu.EtatJeu
import com.getkova.app.jeu.Exercice
import com.getkova.app.jeu.Plage
import com.getkova.app.jeu.Regle
import com.getkova.app.jeu.ajouterPlage
import com.getkova.app.jeu.basculerPlage
import com.getkova.app.jeu.definirApps
import com.getkova.app.jeu.definirHardcore
import com.getkova.app.jeu.estUnRecord
import com.getkova.app.jeu.mettreAJour
import com.getkova.app.jeu.ouvrirFenetre
import com.getkova.app.jeu.proposerRegle
import com.getkova.app.jeu.suspendreFenetre
import com.getkova.app.jeu.utiliserUrgence
import com.getkova.app.jeu.validerSeance
import com.getkova.app.verrou.Minuteur
import com.getkova.app.verrou.Rappel
import kotlinx.serialization.json.Json
import java.time.ZoneId

/**
 * Lecture et écriture de l'état du jeu sur le téléphone. L'app et le service de
 * verrouillage passent tous les deux par ici. Rien ne sort du téléphone.
 */
object Depot {
    private const val FICHIER = "kova"
    private const val CLE_ETAT = "etatJeu"
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: EtatJeu? = null

    fun reglages(contexte: Context): SharedPreferences =
        contexte.applicationContext.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)

    @Synchronized
    fun lire(contexte: Context): EtatJeu {
        cache?.let { return it }
        val texte = reglages(contexte).getString(CLE_ETAT, null)
        // Un enregistrement illisible n'est pas écrasé ici : on repart d'un état neuf en mémoire.
        val etat = texte?.let { runCatching { json.decodeFromString<EtatJeu>(it) }.getOrNull() } ?: EtatJeu()
        cache = etat
        return etat
    }

    @Synchronized
    fun ecrire(contexte: Context, etat: EtatJeu) {
        cache = etat
        reglages(contexte).edit().putString(CLE_ETAT, json.encodeToString(EtatJeu.serializer(), etat)).apply()
    }
}

/** Réponse à "Combien de temps par jour sur ton téléphone ?". */
enum class TempsParJour(val heures: Int, val libelle: Int) {
    MOINS_DE_DEUX(2, R.string.temps_option_moins_de_2),
    TROIS_A_QUATRE(3, R.string.temps_option_3_a_4),
    CINQ_A_SIX(5, R.string.temps_option_5_a_6),
    SEPT_OU_PLUS(7, R.string.temps_option_7_ou_plus);

    /** Jours par an passés sur le téléphone : heures par jour x 365 / 24. */
    val joursParAn: Int get() = Math.round(heures * 365f / 24f)
    val pourcentDeLAnnee: Int get() = Math.round(heures * 100f / 24f)
    val moisSurDixAns: Int get() = Math.round(heures * 120f / 24f)
}

/** Réponses à "Tu veux quoi, au fond ?". */
enum class Objectif(val libelle: Int) {
    MOINS_SCROLLER(R.string.objectif_option_moins_scroller),
    SPORT(R.string.objectif_option_sport),
    CONCENTRATION(R.string.objectif_option_concentration),
    SOMMEIL(R.string.objectif_option_sommeil),
}

val Exercice.nom: Int
    get() = when (this) {
        Exercice.POMPES -> R.string.exercice_pompes
        Exercice.SQUATS -> R.string.exercice_squats
        Exercice.ABDOS -> R.string.exercice_abdos
        Exercice.FENTES -> R.string.exercice_fentes
        Exercice.JUMPING_JACKS -> R.string.exercice_jumping_jacks
    }

/** État de l'app : l'onboarding, les options et l'état du jeu. */
class EtatApp(application: Application) : AndroidViewModel(application) {
    private val contexte: Context get() = getApplication()
    private val reglages = Depot.reglages(application)
    val zone: ZoneId get() = ZoneId.systemDefault()

    var jeu by mutableStateOf(Depot.lire(application).mettreAJour(System.currentTimeMillis()))
        private set

    var onboardingTermine by mutableStateOf(reglages.getBoolean("onboardingTermine", false))
        private set

    var tempsParJour by mutableStateOf(
        runCatching { TempsParJour.valueOf(reglages.getString("tempsParJour", "") ?: "") }.getOrDefault(TempsParJour.CINQ_A_SIX),
    )
        private set

    var objectifs by mutableStateOf(
        (reglages.getStringSet("objectifs", null) ?: setOf(Objectif.MOINS_SCROLLER.name, Objectif.SPORT.name))
            .mapNotNull { nom -> runCatching { Objectif.valueOf(nom) }.getOrNull() }.toSet(),
    )
        private set

    var compterAVoixHaute by mutableStateOf(reglages.getBoolean("compterAVoixHaute", false))
        private set
    var rappelQuotidien by mutableStateOf(reglages.getBoolean("rappelQuotidien", false))
        private set
    var offreDejaVue by mutableStateOf(reglages.getBoolean("offreDejaVue", false))
        private set

    /** Vrai si l'utilisateur a accepté l'envoi de statistiques anonymes. */
    var mesuresAcceptees by mutableStateOf(Mesures.consentement(application) == true)
        private set

    fun definirMesures(acceptees: Boolean) {
        // Le choix est noté avant d'être appliqué : un refus ne laisse partir que lui-même... et rien d'autre.
        if (acceptees) Mesures.definirConsentement(contexte, true)
        Mesures.suivre(contexte, "mesures_choix", "acceptees" to acceptees)
        if (!acceptees) Mesures.definirConsentement(contexte, false)
        mesuresAcceptees = acceptees
    }

    /**
     * Abonnement Google Play actif, d'après la dernière réponse de Google Play.
     * Gardé sur le téléphone : l'app démarre avec, même sans réseau.
     */
    private var abonnementAchete by mutableStateOf(reglages.getBoolean("abonnementAchete", false))

    /** Réglage de chantier : donne accès à tout sans achat. Sans effet dans la version publiée. */
    private var abonnementSimule by mutableStateOf(reglages.getBoolean("abonnementSimule", false))

    val abonne: Boolean get() = abonnementAchete || (BuildConfig.DEBUG && abonnementSimule)

    /** Faux si le dernier achat fait sur ce téléphone est l'abonnement hebdomadaire. */
    var abonnementAnnuel by mutableStateOf(reglages.getBoolean("abonnementAnnuel", true))
        private set

    /** Message d'achat à montrer (identifiant de texte), ou rien. */
    var messageAchat by mutableStateOf<Int?>(null)

    private val filPrincipal = Handler(Looper.getMainLooper())

    /** Les achats Google Play. Ses réponses arrivent sur un autre fil : on revient sur le principal. */
    val achats = Achats(
        application,
        surEtat = { actif ->
            filPrincipal.post {
                if (actif != abonnementAchete) {
                    abonnementAchete = actif
                    reglages.edit().putBoolean("abonnementAchete", actif).apply()
                }
            }
        },
        surAchat = { essai ->
            filPrincipal.post {
                reglages.edit().putBoolean("abonnementAnnuel", abonnementAnnuel).apply()
                // Promis sur le paywall : un rappel la veille de la fin de l'essai.
                if (essai) Rappel.programmerFinEssai(contexte)
                Mesures.suivre(contexte, "achat", "annuel" to abonnementAnnuel, "essai" to essai)
            }
        },
        surProbleme = { message -> filPrincipal.post { messageAchat = message } },
    )

    /** Ouvre la fenêtre d'achat de Google Play pour la formule choisie. */
    fun acheter(activite: Activity, annuel: Boolean) {
        abonnementAnnuel = annuel
        achats.acheter(activite, annuel)
    }

    /** Réglage de chantier : compte une répétition par seconde, sans caméra. */
    var comptageSimule by mutableStateOf(reglages.getBoolean("comptageSimule", false))
        private set

    /** Réglage de chantier : affiche les mesures du comptage pendant la séance. Éteint, l'écran est propre. */
    var mesuresDuComptage by mutableStateOf(reglages.getBoolean("mesuresDuComptage", false))
        private set

    /** Vrai une fois l'autorisation de notifier demandée : on ne la redemande pas. */
    var notificationsDemandees by mutableStateOf(reglages.getBoolean("notificationsDemandees", false))
        private set

    /** Exercice de la séance en cours. */
    var exercice by mutableStateOf(jeu.exerciceParDefaut)

    val regle: Regle get() = jeu.regle
    val repetitionsDeLaSeance: Int get() = jeu.regle.repetitionsDemandees(exercice)

    init {
        Mesures.suivre(application, "app_ouverte", "onboarding_termine" to onboardingTermine)
        achats.demarrer()
    }

    private fun maintenant() = System.currentTimeMillis()

    private fun <T> modifier(changement: (EtatJeu, Long) -> Pair<EtatJeu, T>): T {
        val instant = maintenant()
        val (etat, resultat) = changement(Depot.lire(contexte).mettreAJour(instant), instant)
        if (etat != jeu) {
            Depot.ecrire(contexte, etat)
            jeu = etat
        }
        // La notification du temps restant suit l'état : affichée pendant une fenêtre, retirée sinon.
        Minuteur.mettreAJour(contexte, etat, instant, zone)
        return resultat
    }

    /** Recalcule l'état à l'heure qu'il est (retour dans l'app, échéances passées). */
    fun rafraichir() = modifier { etat, instant ->
        // Kova est à l'écran : on n'est plus dans une app verrouillée, le temps ne se décompte plus.
        etat.suspendreFenetre(instant) to Unit
    }

    fun terminerOnboarding() {
        onboardingTermine = true
        reglages.edit().putBoolean("onboardingTermine", true).apply()
        Mesures.suivre(
            contexte, "onboarding_termine",
            "abonne" to abonne, "apps" to jeu.apps.size, "hardcore" to jeu.hardcore,
            "repetitions" to regle.repetitions, "minutes" to regle.minutes,
        )
    }

    fun revoirOnboarding() {
        onboardingTermine = false
        reglages.edit().putBoolean("onboardingTermine", false).apply()
    }

    fun choisirTemps(choix: TempsParJour) {
        tempsParJour = choix
        reglages.edit().putString("tempsParJour", choix.name).apply()
    }

    fun basculerObjectif(objectif: Objectif) {
        objectifs = if (objectif in objectifs) objectifs - objectif else objectifs + objectif
        reglages.edit().putStringSet("objectifs", objectifs.map { it.name }.toSet()).apply()
    }

    fun definirOption(cle: String, valeur: Boolean) {
        when (cle) {
            "compterAVoixHaute" -> compterAVoixHaute = valeur
            "rappelQuotidien" -> rappelQuotidien = valeur
            "offreDejaVue" -> offreDejaVue = valeur
            "abonnementSimule" -> abonnementSimule = valeur
            "comptageSimule" -> comptageSimule = valeur
            "mesuresDuComptage" -> mesuresDuComptage = valeur
            "notificationsDemandees" -> notificationsDemandees = valeur
        }
        reglages.edit().putBoolean(cle, valeur).apply()
        Mesures.suivre(contexte, "option_changee", "option" to cle, "valeur" to valeur)
    }

    /** Version de test seulement : fenêtre de quelques secondes, pour vérifier le reverrouillage. */
    fun ouvrirFenetreDeTest(secondes: Int) =
        modifier { etat, instant -> etat.copy(fenetreFin = instant + secondes * 1000L) to Unit }

    /** Valide la séance avec les répétitions vraiment faites, au-delà de la série si on a continué. */
    fun validerSeance(repetitions: Int) {
        modifier { etat, instant -> etat.validerSeance(exercice, instant, zone, repetitions) to Unit }
        val seance = jeu.seances.lastOrNull() ?: return
        Mesures.suivre(
            contexte, "seance_validee",
            "exercice" to seance.exercice.name, "repetitions" to seance.repetitions, "minutes" to seance.minutes,
            "serie_demandee" to repetitionsDeLaSeance, "record" to jeu.estUnRecord(), "seances" to jeu.seances.size,
        )
    }

    /** Ouvre une fenêtre avec tout le solde. Null si rien n'a pu s'ouvrir. */
    fun ouvrirFenetre(): Long? {
        val minutes = jeu.soldeMinutes
        val fin = modifier { etat, instant -> etat.ouvrirFenetre(instant, zone) }
        if (fin != null) Mesures.suivre(contexte, "fenetre_ouverte", "minutes" to minutes)
        return fin
    }

    fun utiliserUrgence(): Long? {
        val fin = modifier { etat, instant -> etat.utiliserUrgence(instant, zone) }
        Mesures.suivre(contexte, "urgence_demandee", "accordee" to (fin != null))
        return fin
    }

    /**
     * Change la règle. Avec [immediat], elle s'applique tout de suite. Sans, assouplir
     * est différé de 24 h. Renvoie vrai si le changement est différé.
     */
    fun proposerRegle(regle: Regle, immediat: Boolean, onboarding: Boolean = immediat): Boolean {
        val differe = modifier { etat, instant ->
            if (immediat) {
                // Onboarding et réglages : la règle change tout de suite, dans les deux sens.
                etat.copy(regle = regle, regleEnAttente = null, regleEnAttenteLe = null) to false
            } else {
                etat.proposerRegle(regle, instant)
            }
        }
        Mesures.suivre(
            contexte, "regle_validee",
            "repetitions" to regle.repetitions, "minutes" to regle.minutes, "differe" to differe, "onboarding" to onboarding,
        )
        return differe
    }

    /** Active ou coupe le mode hardcore. Couper prend effet dans 24 h, sauf pendant l'onboarding. */
    fun definirHardcore(actif: Boolean, immediat: Boolean) {
        modifier { etat, instant -> etat.definirHardcore(actif, instant, zone, immediat) to Unit }
        Mesures.suivre(contexte, "hardcore_change", "actif" to actif, "onboarding" to immediat)
    }

    fun definirApps(apps: Set<String>, immediat: Boolean) {
        modifier { etat, instant -> etat.definirApps(apps, instant, immediat) to Unit }
        // Seulement le nombre d'apps : jamais leurs noms.
        Mesures.suivre(contexte, "apps_modifiees", "nombre" to apps.size, "onboarding" to immediat)
    }

    fun basculerPlage(id: String) = modifier { etat, instant -> etat.basculerPlage(id, instant) to Unit }

    fun ajouterPlage(plage: Plage) = modifier { etat, _ -> etat.ajouterPlage(plage) to Unit }

    fun definirUrgenceActivee(active: Boolean) = modifier { etat, _ -> etat.copy(urgenceActivee = active) to Unit }

    fun definirExerciceParDefaut(choix: Exercice) {
        exercice = choix
        modifier { etat, _ -> etat.copy(exerciceParDefaut = choix) to Unit }
    }
}
