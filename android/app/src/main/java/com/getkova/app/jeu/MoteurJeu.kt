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

// --- Séances et solde ---------------------------------------------------------

/** Une séance validée ajoute les minutes de la règle au solde (règles 3 et 9). */
fun EtatJeu.validerSeance(exercice: Exercice, maintenant: Long): EtatJeu {
    val seance = Seance(maintenant, exercice, regle.repetitionsDemandees(exercice), regle.minutes)
    return copy(
        seances = (seances + seance).takeLast(EtatJeu.HISTORIQUE_MAXIMUM),
        soldeMinutes = soldeMinutes + seance.minutes,
    )
}

// --- Planning -----------------------------------------------------------------

fun EtatJeu.plageActive(maintenant: Long, zone: ZoneId): Boolean =
    plages.any { it.enVigueur(maintenant) && it.contient(maintenant, zone) }

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
    if (soldeMinutes <= 0 || plageActive(maintenant, zone)) return this to null
    val depart = if (fenetreOuverte(maintenant)) fenetreFin ?: maintenant else maintenant
    val fin = depart + soldeMinutes * 60_000L
    return copy(fenetreFin = fin, soldeMinutes = 0) to fin
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

/**
 * Jours d'affilée avec au moins une séance validée (règle 8).
 * La série tient si la dernière séance date d'aujourd'hui ou d'hier.
 */
fun EtatJeu.serie(maintenant: Long, zone: ZoneId): Int {
    val jours = seances.map { Semaine.date(it.date, zone).toLocalDate() }.toSet()
    var jour = Semaine.date(maintenant, zone).toLocalDate()
    if (jour !in jours) {
        jour = jour.minusDays(1)
        if (jour !in jours) return 0
    }
    var compte = 0
    while (jour in jours) {
        compte += 1
        jour = jour.minusDays(1)
    }
    return compte
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
    val avant = seances.filter { it.date in lundiPrecedent until lundi }.sumOf { it.repetitions }
    return BilanSemaine(
        repetitionsParJour = parJour,
        rangAujourdHui = Semaine.jour(maintenant, zone) - 1,
        seances = courantes.size,
        minutesGagnees = courantes.sumOf { it.minutes },
        progressionPourcent = if (avant > 0) ((total - avant) * 100.0 / avant).roundToInt() else null,
    )
}
