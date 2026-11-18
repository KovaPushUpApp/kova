package com.getkova.app.jeu

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.roundToInt

// Les règles du jeu (brief, section 6). Chaque fonction reçoit l'heure et le
// fuseau, et renvoie un nouvel état : rien n'est modifié en place.

object Semaine {
    fun date(instant: Long, zone: ZoneId): ZonedDateTime = Instant.ofEpochMilli(instant).atZone(zone)

    /** Jour de la semaine : 1 = lundi, 7 = dimanche. */
    fun jour(instant: Long, zone: ZoneId): Int = date(instant, zone).dayOfWeek.value

    fun debutDuJour(jour: LocalDate, zone: ZoneId): Long = jour.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Lundi 0 h de la semaine qui contient l'instant. */
    fun debut(instant: Long, zone: ZoneId): Long {
        val jour = date(instant, zone).toLocalDate()
        return debutDuJour(jour.minusDays((jour.dayOfWeek.value - 1).toLong()), zone)
    }

    /** Le prochain minuit après l'instant. */
    fun minuitSuivant(instant: Long, zone: ZoneId): Long =
        debutDuJour(date(instant, zone).toLocalDate().plusDays(1), zone)
}

// --- Plages -----------------------------------------------------------------

/** Vrai si la plage s'applique encore : active, ou désactivée depuis moins de 24 h. */
fun Plage.enVigueur(maintenant: Long): Boolean = active || (enVigueurJusquA?.let { maintenant < it } ?: false)

/** Vrai si l'instant tombe dans la plage. */
fun Plage.contient(maintenant: Long, zone: ZoneId): Boolean {
    val date = Semaine.date(maintenant, zone)
    val minute = date.hour * 60 + date.minute
    val jour = date.dayOfWeek.value
    if (debut < fin) {
        return jour in jours && minute >= debut && minute < fin
    }
    // La plage passe minuit : elle a commencé aujourd'hui ou hier.
    val veille = if (jour == 1) 7 else jour - 1
    return (jour in jours && minute >= debut) || (veille in jours && minute < fin)
}

// --- Règle ------------------------------------------------------------------

/** Applique ce qui est arrivé à échéance. À appeler avant de lire l'état. */
fun EtatJeu.mettreAJour(maintenant: Long): EtatJeu {
    var etat = this
    val attente = regleEnAttente
    val le = regleEnAttenteLe
    if (attente != null && le != null && maintenant >= le) {
        etat = etat.copy(regle = attente, regleEnAttente = null, regleEnAttenteLe = null)
    }
    // Mode hardcore : à minuit, le temps gagné est perdu. La fenêtre est déjà bornée à minuit.
    val effacement = etat.effacementLe
    if (effacement != null && maintenant >= effacement) {
        val efface = etat.hardcoreEnVigueur(effacement)
        etat = etat.copy(
            soldeMinutes = if (efface) 0 else etat.soldeMinutes,
            soldeSecondes = if (efface) 0 else etat.soldeSecondes,
            effacementLe = null,
        )
    }
    if (etat.hardcoreJusquA?.let { it <= maintenant } == true) {
        etat = etat.copy(hardcoreJusquA = null)
    }
    if (etat.fenetreFin?.let { it <= maintenant } == true) {
        etat = etat.copy(fenetreFin = null)
    }
    if (etat.urgenceFin?.let { it <= maintenant } == true) {
        etat = etat.copy(urgenceFin = null)
    }
    if (etat.plages.any { p -> p.enVigueurJusquA?.let { it <= maintenant } == true }) {
        etat = etat.copy(plages = etat.plages.map { p ->
            if (p.enVigueurJusquA?.let { it <= maintenant } == true) p.copy(enVigueurJusquA = null) else p
        })
    }
    if (etat.appsRetirees.any { it.value <= maintenant }) {
        etat = etat.copy(appsRetirees = etat.appsRetirees.filter { it.value > maintenant })
    }
    return etat
}

/**
 * Change la règle. Durcir prend effet tout de suite, assouplir dans 24 h (règle 7).
 * Renvoie le nouvel état, et vrai si le changement est différé.
 */
fun EtatJeu.proposerRegle(nouvelle: Regle, maintenant: Long): Pair<EtatJeu, Boolean> {
    if (nouvelle == regle) {
        return copy(regleEnAttente = null, regleEnAttenteLe = null) to false
    }
    if (nouvelle.minutesParPompe > regle.minutesParPompe) {
        return copy(
            regleEnAttente = nouvelle,
            regleEnAttenteLe = maintenant + EtatJeu.DELAI_ASSOUPLISSEMENT,
        ) to true
    }
    return copy(regle = nouvelle, regleEnAttente = null, regleEnAttenteLe = null) to false
}

// --- Mode hardcore --------------------------------------------------------------

/** Vrai si le mode hardcore s'applique à cet instant : activé, ou coupé depuis moins de 24 h. */
fun EtatJeu.hardcoreEnVigueur(instant: Long): Boolean = hardcore || (hardcoreJusquA?.let { instant < it } ?: false)

/**
 * Programme l'effacement au prochain minuit et y arrête la fenêtre ouverte.
 * Sans effet si le mode hardcore ne s'applique plus à ce minuit-là.
 */
private fun EtatJeu.bornerAMinuit(maintenant: Long, zone: ZoneId): EtatJeu {
    val minuit = Semaine.minuitSuivant(maintenant, zone)
    if (!hardcoreEnVigueur(minuit)) return copy(effacementLe = null)
    return copy(effacementLe = minuit, fenetreFin = fenetreFin?.coerceAtMost(minuit))
}

/**
 * Active ou coupe le mode hardcore. Activer est immédiat, couper prend effet
 * dans 24 h (règle 7). `immediat` sert à l'onboarding.
 */
fun EtatJeu.definirHardcore(actif: Boolean, maintenant: Long, zone: ZoneId, immediat: Boolean): EtatJeu = when {
    actif -> copy(hardcore = true, hardcoreJusquA = null)
    immediat || !hardcoreEnVigueur(maintenant) -> copy(hardcore = false, hardcoreJusquA = null)
    hardcore -> copy(hardcore = false, hardcoreJusquA = maintenant + EtatJeu.DELAI_ASSOUPLISSEMENT)
    else -> this
}.bornerAMinuit(maintenant, zone)

// --- Séances et solde ---------------------------------------------------------

/**
 * Une séance validée ajoute ses minutes au solde (règles 3 et 9). On peut dépasser
 * la série de la règle : les répétitions en plus rapportent au même rapport.
 */
fun EtatJeu.validerSeance(
    exercice: Exercice,
    maintenant: Long,
    zone: ZoneId,
    repetitions: Int = regle.repetitionsDemandees(exercice),
): EtatJeu {
    val faites = max(repetitions, regle.repetitionsDemandees(exercice))
    val seance = Seance(maintenant, exercice, faites, regle.minutesPour(exercice, faites))
    return copy(
        seances = (seances + seance).takeLast(EtatJeu.HISTORIQUE_MAXIMUM),
        soldeMinutes = soldeMinutes + seance.minutes,
    ).bornerAMinuit(maintenant, zone)
}

// --- Planning -----------------------------------------------------------------

/** La plage stricte en cours, s'il y en a une. */
fun EtatJeu.plageEnCours(maintenant: Long, zone: ZoneId): Plage? =
    plages.firstOrNull { it.enVigueur(maintenant) && it.contient(maintenant, zone) }

fun EtatJeu.plageActive(maintenant: Long, zone: ZoneId): Boolean = plageEnCours(maintenant, zone) != null

/** Activer est immédiat, désactiver prend effet dans 24 h (règle 7). */
fun EtatJeu.basculerPlage(id: String, maintenant: Long): EtatJeu = copy(plages = plages.map { plage ->
    when {
        plage.id != id -> plage
        plage.active -> plage.copy(active = false, enVigueurJusquA = maintenant + EtatJeu.DELAI_ASSOUPLISSEMENT)
        else -> plage.copy(active = true, enVigueurJusquA = null)
    }
})

/** Ajouter une plage durcit le jeu : effet immédiat. */
fun EtatJeu.ajouterPlage(plage: Plage): EtatJeu =
    if (plages.size >= Plage.NOMBRE_MAXIMUM) this else copy(plages = plages + plage)

// --- Fenêtre de déblocage -------------------------------------------------------
//
// Le temps gagné ne se décompte que pendant qu'on utilise une app verrouillée.
// Ouvrir une de ces apps démarre la fenêtre avec tout le solde ; la quitter
// suspend la fenêtre, et ce qui reste retourne au solde, à la seconde près.

/** Tout le solde, en secondes. */
val EtatJeu.soldeEnSecondes: Int get() = soldeMinutes * 60 + soldeSecondes

/** Le solde à afficher, arrondi à la minute au-dessus : il reste "1 min" jusqu'à la dernière seconde. */
val EtatJeu.minutesAffichees: Int get() = soldeMinutes + if (soldeSecondes > 0) 1 else 0

/** Arrête la fenêtre en cours et remet au solde le temps qui n'a pas été utilisé. */
fun EtatJeu.suspendreFenetre(maintenant: Long): EtatJeu {
    val fin = fenetreFin ?: return this
    if (fin <= maintenant) return copy(fenetreFin = null)
    val total = soldeEnSecondes + ((fin - maintenant) / 1000).toInt()
    return copy(fenetreFin = null, soldeMinutes = total / 60, soldeSecondes = total % 60)
}

fun EtatJeu.fenetreOuverte(maintenant: Long): Boolean = fenetreFin?.let { it > maintenant } ?: false

fun EtatJeu.urgenceEnCours(maintenant: Long): Boolean = urgenceFin?.let { it > maintenant } ?: false

/** Vrai si les apps verrouillées sont accessibles maintenant (règles 4, 5 et 6). */
fun EtatJeu.appsOuvertes(maintenant: Long, zone: ZoneId): Boolean =
    urgenceEnCours(maintenant) || (fenetreOuverte(maintenant) && !plageActive(maintenant, zone))

/** Début de la prochaine plage stricte après l'instant donné, dans les 8 jours. */
fun EtatJeu.prochainDebutDePlage(maintenant: Long, zone: ZoneId): Long? {
    val aujourdHui = Semaine.date(maintenant, zone).toLocalDate()
    return plages.filter { it.enVigueur(maintenant) }.mapNotNull { plage ->
        (0L..7L).firstNotNullOfOrNull { decalage ->
            val jour = aujourdHui.plusDays(decalage)
            val debut = jour.atStartOfDay(zone).plusMinutes(plage.debut.toLong()).toInstant().toEpochMilli()
            if (jour.dayOfWeek.value in plage.jours && debut > maintenant) debut else null
        }
    }.minOrNull()
}

/**
 * Instant où les apps se reverrouillent, si elles sont ouvertes.
 * Une plage stricte coupe la fenêtre, mais pas le déblocage d'urgence.
 */
fun EtatJeu.finDOuverture(maintenant: Long, zone: ZoneId): Long? {
    if (!appsOuvertes(maintenant, zone)) return null
    val urgence = if (urgenceEnCours(maintenant)) urgenceFin else null
    if (plageActive(maintenant, zone)) return urgence
    var fin = if (fenetreOuverte(maintenant)) fenetreFin else null
    val plage = prochainDebutDePlage(maintenant, zone)
    if (fin != null && plage != null && plage < fin) {
        fin = plage
    }
    return listOfNotNull(fin, urgence).maxOrNull()
}

/**
 * Démarre une fenêtre avec tout le solde. Une fenêtre déjà ouverte est prolongée.
 * Refusé pendant une plage stricte (règle 5). Renvoie le nouvel état et la fin
 * de la fenêtre, ou null si rien n'a été ouvert.
 */
fun EtatJeu.ouvrirFenetre(maintenant: Long, zone: ZoneId): Pair<EtatJeu, Long?> {
    if (soldeEnSecondes <= 0 || plageActive(maintenant, zone)) return this to null
    val depart = if (fenetreOuverte(maintenant)) fenetreFin ?: maintenant else maintenant
    // En mode hardcore, la fenêtre s'arrête à minuit : le reste est perdu.
    val etat = copy(fenetreFin = depart + soldeEnSecondes * 1000L, soldeMinutes = 0, soldeSecondes = 0)
        .bornerAMinuit(maintenant, zone)
    return etat to etat.fenetreFin
}

// --- Urgence --------------------------------------------------------------------

/** Déblocages d'urgence encore possibles cette semaine. Remise à zéro le lundi (règle 6). */
fun EtatJeu.urgencesRestantes(maintenant: Long, zone: ZoneId): Int {
    val lundi = Semaine.debut(maintenant, zone)
    val utilisees = urgences.count { it in lundi..maintenant }
    return max(0, EtatJeu.URGENCES_PAR_SEMAINE - utilisees)
}

/**
 * Ouvre 5 minutes sans exercice. Passe même pendant une plage stricte.
 * Renvoie le nouvel état et la fin du déblocage, ou null si l'urgence n'est pas disponible.
 */
fun EtatJeu.utiliserUrgence(maintenant: Long, zone: ZoneId): Pair<EtatJeu, Long?> {
    if (!urgenceActivee || urgencesRestantes(maintenant, zone) <= 0) return this to null
    val lundi = Semaine.debut(maintenant, zone)
    val fin = maintenant + EtatJeu.MINUTES_URGENCE * 60_000L
    return copy(urgences = urgences.filter { it >= lundi } + maintenant, urgenceFin = fin) to fin
}

// --- Apps -----------------------------------------------------------------------

/**
 * Enregistre les apps choisies. Ajouter une app prend effet tout de suite.
 * Retirer une app prend effet 24 h plus tard (règle 7) : elle reste verrouillée
 * jusque-là. `immediat` sert à l'onboarding, avant le premier verrouillage.
 */
fun EtatJeu.definirApps(nouvelles: Set<String>, maintenant: Long, immediat: Boolean): EtatJeu {
    if (immediat) return copy(apps = nouvelles, appsRetirees = emptyMap())
    val limite = maintenant + EtatJeu.DELAI_ASSOUPLISSEMENT
    val retirees = appsRetirees.toMutableMap()
    for (app in apps - nouvelles) {
        // Une app déjà en sursis garde son échéance d'origine.
        retirees.putIfAbsent(app, limite)
    }
    // Une app remise dans la liste n'est plus en sursis.
    nouvelles.forEach { retirees.remove(it) }
    return copy(apps = nouvelles, appsRetirees = retirees)
}

/** Les apps sous verrou : les apps choisies et celles retirées depuis moins de 24 h. */
fun EtatJeu.appsSousVerrou(maintenant: Long): Set<String> =
    apps + appsRetirees.filter { it.value > maintenant }.keys

/** Vrai si cette app doit être bloquée maintenant. */
fun EtatJeu.estBloquee(paquet: String, maintenant: Long, zone: ZoneId): Boolean =
    paquet in appsSousVerrou(maintenant) && !appsOuvertes(maintenant, zone)

// --- Série et stats -------------------------------------------------------------

/** Ce que donne le parcours de la série : les jours comptés et les jours sauvés par un joker. */
private class Parcours(val jours: Int, val jokers: Set<LocalDate>)

/**
 * Remonte les jours depuis aujourd'hui. Aujourd'hui sans séance ne casse rien :
 * la journée n'est pas finie. Ensuite un jour sans séance est pardonné par le
 * joker, une fois par semaine (du lundi au dimanche). Un joker ne compte que s'il
 * relie deux jours avec séance.
 */
private fun EtatJeu.parcourirSerie(maintenant: Long, zone: ZoneId): Parcours {
    val faits = seances.map { Semaine.date(it.date, zone).toLocalDate() }.toSet()
    val premier = faits.minOrNull() ?: return Parcours(0, emptySet())
    var jour = Semaine.date(maintenant, zone).toLocalDate()
    if (jour !in faits) jour = jour.minusDays(1)
    var compte = 0
    val jokers = mutableSetOf<LocalDate>()
    val enAttente = mutableListOf<LocalDate>()
    val semainesServies = mutableSetOf<LocalDate>()
    while (jour >= premier) {
        if (jour in faits) {
            compte += 1
            jokers += enAttente
            enAttente.clear()
        } else {
            val lundi = jour.minusDays((jour.dayOfWeek.value - 1).toLong())
            if (!semainesServies.add(lundi)) break
            enAttente += jour
        }
        jour = jour.minusDays(1)
    }
    return Parcours(compte, jokers)
}

/**
 * Jours avec au moins une séance validée, d'affilée (règle 8). La série tient si
 * la dernière séance date d'aujourd'hui ou d'hier. Un joker par semaine pardonne
 * un jour raté : ce jour ne compte pas, mais la série continue.
 */
fun EtatJeu.serie(maintenant: Long, zone: ZoneId): Int = parcourirSerie(maintenant, zone).jours

/** Vrai si le joker de cette semaine a déjà sauvé la série. */
fun EtatJeu.jokerUtilise(maintenant: Long, zone: ZoneId): Boolean {
    val lundi = Semaine.date(Semaine.debut(maintenant, zone), zone).toLocalDate()
    return parcourirSerie(maintenant, zone).jokers.any { it >= lundi }
}

/** Vrai si la dernière séance bat le record de répétitions de son exercice. */
fun EtatJeu.estUnRecord(): Boolean {
    val derniere = seances.lastOrNull() ?: return false
    val avant = seances.dropLast(1).filter { it.exercice == derniere.exercice }
    return avant.isNotEmpty() && derniere.repetitions > avant.maxOf { it.repetitions }
}

fun EtatJeu.repetitionsDuJour(maintenant: Long, zone: ZoneId): Int {
    val jour = Semaine.date(maintenant, zone).toLocalDate()
    return seances.filter { Semaine.date(it.date, zone).toLocalDate() == jour }.sumOf { it.repetitions }
}

/** Bilan de la semaine en cours, du lundi au dimanche. */
fun EtatJeu.bilanDeLaSemaine(maintenant: Long, zone: ZoneId): BilanSemaine {
    val lundi = Semaine.debut(maintenant, zone)
    val lundiSuivant = Semaine.date(lundi, zone).plusDays(7).toInstant().toEpochMilli()
    val lundiPrecedent = Semaine.date(lundi, zone).minusDays(7).toInstant().toEpochMilli()
    val courantes = seances.filter { it.date in lundi until lundiSuivant }
    val parJour = MutableList(7) { 0 }
    for (seance in courantes) {
        parJour[Semaine.jour(seance.date, zone) - 1] += seance.repetitions
    }
    val total = parJour.sum()
    // On compare à jours égaux : du lundi d'avant jusqu'au même jour de la semaine
    // d'avant, inclus. Sinon une semaine en cours paraît toujours en baisse.
    val borne = Semaine.date(maintenant, zone).toLocalDate().plusDays(1).minusDays(7)
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val avant = seances.filter { it.date in lundiPrecedent until borne }.sumOf { it.repetitions }
    return BilanSemaine(
        repetitionsParJour = parJour,
        rangAujourdHui = Semaine.jour(maintenant, zone) - 1,
        seances = courantes.size,
        minutesGagnees = courantes.sumOf { it.minutes },
        progressionPourcent = if (avant > 0) ((total - avant) * 100.0 / avant).roundToInt() else null,
    )
}
