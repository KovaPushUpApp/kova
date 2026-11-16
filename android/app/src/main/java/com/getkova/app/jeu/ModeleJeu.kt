package com.getkova.app.jeu

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

// Modèle des règles du jeu (brief, section 6). Kotlin pur, sans Android :
// tout se teste sur l'ordinateur. Les instants sont en millisecondes Unix.

/**
 * Les exercices de Kova. Le taux de base est le nombre de répétitions
 * demandé quand la règle vaut 10 pompes (règle 2).
 */
@Serializable
enum class Exercice(val tauxDeBase: Int) {
    POMPES(10), SQUATS(15), ABDOS(15), FENTES(12), JUMPING_JACKS(25);

    companion object {
        /** Exercices dont le comptage est livré. */
        val disponibles = listOf(POMPES, SQUATS)

        /** Exercices utilisables sans abonnement (brief, section 4). */
        val gratuits = listOf(POMPES)
    }
}

/** La règle : X pompes donnent Y minutes (règle 1). */
@Serializable
data class Regle(val repetitions: Int = 10, val minutes: Int = 15) {
    fun changerRepetitions(pas: Int) = creer(repetitions + pas, minutes)

    fun changerMinutes(pas: Int) = creer(repetitions, minutes + pas)

    /** Pompes à faire pour gagner une heure. */
    val pompesParHeure: Int get() = (repetitions * 60.0 / minutes).roundToInt()

    /** Minutes gagnées par pompe. Plus c'est haut, plus la règle est souple. */
    val minutesParPompe: Double get() = minutes.toDouble() / repetitions

    /** Répétitions demandées pour un exercice : même rapport que les pompes. */
    fun repetitionsDemandees(exercice: Exercice): Int =
        max(1, (exercice.tauxDeBase.toDouble() * repetitions / Exercice.POMPES.tauxDeBase).roundToInt())

    /**
     * Minutes gagnées pour une séance. Rien tant que la série de la règle n'est pas
     * faite. Ensuite chaque répétition compte, au même rapport, arrondi à la minute en dessous.
     */
    fun minutesPour(exercice: Exercice, repetitions: Int): Int {
        val serie = repetitionsDemandees(exercice)
        return if (repetitions < serie) 0 else repetitions * minutes / serie
    }

    companion object {
        const val PAS = 5
        val repetitionsPossibles = 5..100
        val minutesPossibles = 5..120
        val parDefaut = Regle()

        /** Crée une règle en la ramenant dans les bornes permises. */
        fun creer(repetitions: Int, minutes: Int) = Regle(
            repetitions.coerceIn(repetitionsPossibles),
            minutes.coerceIn(minutesPossibles),
        )
    }
}

/** Une plage stricte du planning (règle 5). */
@Serializable
data class Plage(
    val id: String,
    val nom: String,
    /** Début et fin, en minutes depuis minuit. Si la fin est avant le début, la plage passe minuit. */
    val debut: Int,
    val fin: Int,
    /** Jours où la plage commence : 1 = lundi, 7 = dimanche. */
    val jours: Set<Int> = tousLesJours,
    val active: Boolean = true,
    /** Une plage désactivée reste en vigueur jusqu'à cet instant (règle 7). */
    val enVigueurJusquA: Long? = null,
) {
    companion object {
        val tousLesJours = setOf(1, 2, 3, 4, 5, 6, 7)
        const val NOMBRE_MAXIMUM = 8
    }
}

/** Une séance validée. */
@Serializable
data class Seance(val date: Long, val exercice: Exercice, val repetitions: Int, val minutes: Int)

/** Tout l'état du jeu, enregistré sur le téléphone. */
@Serializable
data class EtatJeu(
    val regle: Regle = Regle.parDefaut,
    /** Règle plus souple en attente, et l'instant où elle s'applique. */
    val regleEnAttente: Regle? = null,
    val regleEnAttenteLe: Long? = null,
    val soldeMinutes: Int = 0,
    /** Mode hardcore : le temps gagné et la fenêtre ouverte s'effacent chaque jour à minuit. */
    val hardcore: Boolean = false,
    /** Un mode hardcore coupé reste en vigueur jusqu'à cet instant (règle 7). */
    val hardcoreJusquA: Long? = null,
    /** Le minuit où le temps gagné s'efface, quand le mode hardcore l'a programmé. */
    val effacementLe: Long? = null,
    /** Fin de la fenêtre de déblocage en cours. */
    val fenetreFin: Long? = null,
    /** Fin du déblocage d'urgence en cours. */
    val urgenceFin: Long? = null,
    /** Instants des déblocages d'urgence utilisés. */
    val urgences: List<Long> = emptyList(),
    val urgenceActivee: Boolean = true,
    val plages: List<Plage> = emptyList(),
    val seances: List<Seance> = emptyList(),
    val exerciceParDefaut: Exercice = Exercice.POMPES,
    /** Apps choisies, par nom de paquet Android. */
    val apps: Set<String> = emptySet(),
    /** Apps retirées de la liste, encore verrouillées jusqu'à l'instant indiqué (règle 7). */
    val appsRetirees: Map<String, Long> = emptyMap(),
) {
    companion object {
        const val MINUTES_URGENCE = 5
        const val URGENCES_PAR_SEMAINE = 2

        /** Délai des assouplissements (règle 7), en millisecondes. */
        const val DELAI_ASSOUPLISSEMENT = 24L * 3600 * 1000

        /** Nombre de séances gardées en mémoire. */
        const val HISTORIQUE_MAXIMUM = 600
    }
}

/** Chiffres de l'écran Stats. */
data class BilanSemaine(
    /** Du lundi (rang 0) au dimanche (rang 6). */
    val repetitionsParJour: List<Int>,
    val rangAujourdHui: Int,
    val seances: Int,
    val minutesGagnees: Int,
    /** Évolution du total par rapport à la semaine précédente. Null s'il n'y a pas de comparaison. */
    val progressionPourcent: Int?,
) {
    val total: Int get() = repetitionsParJour.sum()
}
