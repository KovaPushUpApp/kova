package com.getkova.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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
import com.getkova.app.jeu.mettreAJour
import com.getkova.app.jeu.ouvrirFenetre
import com.getkova.app.jeu.proposerRegle
import com.getkova.app.jeu.utiliserUrgence
import com.getkova.app.jeu.validerSeance
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

    /**
     * Abonnement. Les achats Google Play ne sont pas encore branchés : ce réglage
     * de chantier sert à essayer les fonctions réservées.
     */
    var abonne by mutableStateOf(reglages.getBoolean("abonnementSimule", false))
        private set

    /** Réglage de chantier : compte une répétition par seconde, sans caméra. */
    var comptageSimule by mutableStateOf(reglages.getBoolean("comptageSimule", false))
        private set

    /** Exercice de la séance en cours. */
    var exercice by mutableStateOf(jeu.exerciceParDefaut)

    val regle: Regle get() = jeu.regle
    val repetitionsDeLaSeance: Int get() = jeu.regle.repetitionsDemandees(exercice)

    private fun maintenant() = System.currentTimeMillis()

    private fun <T> modifier(changement: (EtatJeu, Long) -> Pair<EtatJeu, T>): T {
        val instant = maintenant()
        val (etat, resultat) = changement(Depot.lire(contexte).mettreAJour(instant), instant)
        if (etat != jeu) {
            Depot.ecrire(contexte, etat)
            jeu = etat
        }
        return resultat
    }

    /** Recalcule l'état à l'heure qu'il est (retour dans l'app, échéances passées). */
    fun rafraichir() = modifier { etat, _ -> etat to Unit }

    fun terminerOnboarding() {
        onboardingTermine = true
        reglages.edit().putBoolean("onboardingTermine", true).apply()
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
            "abonnementSimule" -> abonne = valeur
            "comptageSimule" -> comptageSimule = valeur
        }
        reglages.edit().putBoolean(cle, valeur).apply()
    }

    /** Version de test seulement : fenêtre de quelques secondes, pour vérifier le reverrouillage. */
    fun ouvrirFenetreDeTest(secondes: Int) =
        modifier { etat, instant -> etat.copy(fenetreFin = instant + secondes * 1000L) to Unit }

    fun validerSeance() = modifier { etat, instant -> etat.validerSeance(exercice, instant) to Unit }

    /** Ouvre une fenêtre avec tout le solde. Null si rien n'a pu s'ouvrir. */
    fun ouvrirFenetre(): Long? = modifier { etat, instant -> etat.ouvrirFenetre(instant, zone) }

    fun utiliserUrgence(): Long? = modifier { etat, instant -> etat.utiliserUrgence(instant, zone) }

    /** Change la règle. Renvoie vrai si le changement est différé de 24 h. */
    fun proposerRegle(regle: Regle, immediat: Boolean): Boolean = modifier { etat, instant ->
        if (immediat) {
            // Pendant l'onboarding, il n'y a pas encore de règle à protéger.
            etat.copy(regle = regle, regleEnAttente = null, regleEnAttenteLe = null) to false
        } else {
            etat.proposerRegle(regle, instant)
        }
    }

    fun definirApps(apps: Set<String>, immediat: Boolean) =
        modifier { etat, instant -> etat.definirApps(apps, instant, immediat) to Unit }

    fun basculerPlage(id: String) = modifier { etat, instant -> etat.basculerPlage(id, instant) to Unit }

    fun ajouterPlage(plage: Plage) = modifier { etat, _ -> etat.ajouterPlage(plage) to Unit }

    fun definirUrgenceActivee(active: Boolean) = modifier { etat, _ -> etat.copy(urgenceActivee = active) to Unit }

    fun definirExerciceParDefaut(choix: Exercice) {
        exercice = choix
        modifier { etat, _ -> etat.copy(exerciceParDefaut = choix) to Unit }
    }
}
