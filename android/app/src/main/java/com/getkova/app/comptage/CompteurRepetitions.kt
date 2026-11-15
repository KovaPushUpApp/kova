package com.getkova.app.comptage

import com.getkova.app.jeu.Exercice
import kotlin.math.acos
import kotlin.math.hypot

/** Un point de l'image. Une unité vaut la hauteur de l'image, pour ne pas déformer les angles. */
data class Point(val x: Double, val y: Double)

/** Les articulations utiles au comptage. */
enum class Articulation {
    EPAULE_GAUCHE, COUDE_GAUCHE, POIGNET_GAUCHE, HANCHE_GAUCHE, GENOU_GAUCHE, CHEVILLE_GAUCHE,
    EPAULE_DROITE, COUDE_DROIT, POIGNET_DROIT, HANCHE_DROITE, GENOU_DROIT, CHEVILLE_DROITE,
}

/** Un côté du corps, vu comme une chaîne d'articulations. */
private data class Cote(
    val epaule: Articulation,
    val coude: Articulation,
    val poignet: Articulation,
    val hanche: Articulation,
    val genou: Articulation,
    val cheville: Articulation,
)

private val cotes = listOf(
    Cote(
        Articulation.EPAULE_GAUCHE, Articulation.COUDE_GAUCHE, Articulation.POIGNET_GAUCHE,
        Articulation.HANCHE_GAUCHE, Articulation.GENOU_GAUCHE, Articulation.CHEVILLE_GAUCHE,
    ),
    Cote(
        Articulation.EPAULE_DROITE, Articulation.COUDE_DROIT, Articulation.POIGNET_DROIT,
        Articulation.HANCHE_DROITE, Articulation.GENOU_DROIT, Articulation.CHEVILLE_DROITE,
    ),
)

/** La posture détectée sur une image : la position des articulations fiables. Aucune image n'est gardée. */
typealias Pose = Map<Articulation, Point>

object Geometrie {
    fun distance(a: Point, b: Point): Double = hypot(a.x - b.x, a.y - b.y)

    /** Angle au sommet `b`, entre les segments b-a et b-c, en degrés (0 à 180). */
    fun angle(a: Point, b: Point, c: Point): Double {
        val v1x = a.x - b.x
        val v1y = a.y - b.y
        val v2x = c.x - b.x
        val v2y = c.y - b.y
        val norme = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (norme <= 0) return 180.0
        val cosinus = ((v1x * v2x + v1y * v2y) / norme).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosinus))
    }
}

/**
 * Seuils du comptage. Les angles viennent du brief (section 9), le reste d'un
 * premier réglage raisonnable. À ajuster sur un vrai téléphone : tant que le
 * critère 3 du brief n'est pas mesuré, ces valeurs sont une hypothèse.
 */
object ReglagesComptage {
    /**
     * Pompe : bas quand le coude passe sous le seuil bas, haut quand il repasse
     * au-dessus du seuil haut. Le brief donne 90° et 160°, mesurés de profil.
     * Téléphone posé devant soi, les bras sont vus de face et l'angle visible est
     * moins marqué : on prend 105° et 145° en attendant un réglage sur un vrai téléphone.
     */
    const val COUDE_BAS = 105.0
    const val COUDE_HAUT = 145.0

    /** Squat : bas quand le genou passe sous 100°, haut quand il repasse au-dessus de 160°. */
    const val GENOU_BAS = 100.0
    const val GENOU_HAUT = 160.0

    /** Le corps doit rester droit : angle épaule, hanche, cheville au minimum. */
    const val ALIGNEMENT_MINIMUM = 140.0

    /**
     * L'alignement ne se juge que de profil : quand l'écart horizontal entre
     * l'épaule et les pieds dépasse cette part de la longueur du bras. De face,
     * les hanches et les pieds sont cachés derrière les épaules.
     */
    const val PROFIL_MINIMUM = 0.75

    /**
     * Entre le haut et le bas, l'épaule (pompe) ou la hanche (squat) doit bouger
     * d'au moins cette part du membre. Vue de face, l'épaule se rapproche de la
     * caméra plus qu'elle ne descend dans l'image : le seuil reste bas.
     */
    const val DEPLACEMENT_MINIMUM = 0.15

    /**
     * Et l'appui (poignet ou cheville) doit rester à peu près en place. Près du
     * sol, la position détectée des poignets tremble : on tolère un écart large.
     */
    const val APPUI_MAXIMUM = 0.4

    /**
     * Nombre d'images sur lesquelles l'angle est lissé. Trop d'images retardent
     * la mesure et écrasent le creux d'une pompe rapide.
     */
    const val LISSAGE = 3

    /** En dessous de cet écart sous le seuil haut, un petit mouvement n'est pas une tentative. */
    const val TENTATIVE_MINIMUM = 20.0

    /** Images sans corps avant de mettre en pause. */
    const val IMAGES_AVANT_PAUSE = 12

    /** Confiance minimale d'une articulation détectée. */
    const val CONFIANCE_MINIMUM = 0.5f
}

/**
 * Compte les répétitions à partir des postures successives.
 * Une répétition ne compte que si le mouvement est complet : haut, bas, puis haut.
 */
class CompteurRepetitions(private val exercice: Exercice) {
    private enum class Phase { INCONNUE, HAUT, BAS }

    private class Mesure(
        /** Angle suivi : coude pour la pompe, genou pour le squat. */
        val angle: Double,
        /** Point qui doit bouger : épaule (pompe) ou hanche (squat). */
        val mobile: Point,
        /** Point d'appui qui doit rester en place : poignet ou cheville. */
        val appui: Point,
        /** Longueur du membre, qui sert d'échelle. */
        val longueur: Double,
        val postureCorrecte: Boolean,
    )

    var compte = 0
        private set

    /** Faux quand le corps est sorti du cadre : la séance est en pause. */
    var corpsVisible = false
        private set

    /** Dernier angle mesuré (coude ou genou), lissé. Sert au réglage sur un vrai téléphone. */
    var angle: Double? = null
        private set

    /** Ce qui est arrivé à la dernière répétition, comptée ou refusée. Pour le réglage. */
    var diagnostic: Diagnostic? = null
        private set

    private var angleLePlusBas = 180.0
    private var deplacementMaximum = 0.0
    private var glissementMaximum = 0.0

    private var phase = Phase.INCONNUE
    private val angles = ArrayDeque<Double>()
    private var imagesSansCorps = 0
    private var mobileEnHaut: Point? = null
    private var appuiEnHaut: Point? = null
    private var longueurEnHaut = 0.0

    /** Traite la posture d'une image. Renvoie vrai si une répétition vient d'être comptée. */
    fun traiter(pose: Pose?): Boolean {
        val mesure = pose?.let { mesurer(it) }
        if (mesure == null) {
            imagesSansCorps += 1
            if (imagesSansCorps >= ReglagesComptage.IMAGES_AVANT_PAUSE) {
                corpsVisible = false
                angles.clear()
            }
            return false
        }
        imagesSansCorps = 0
        corpsVisible = true

        // Faux mouvement : on ignore l'image, sans casser la série en cours.
        if (!mesure.postureCorrecte) return false

        angles.addLast(mesure.angle)
        while (angles.size > ReglagesComptage.LISSAGE) angles.removeFirst()
        val angle = angles.average()
        this.angle = angle

        val squat = exercice == Exercice.SQUATS
        val seuilBas = if (squat) ReglagesComptage.GENOU_BAS else ReglagesComptage.COUDE_BAS
        val seuilHaut = if (squat) ReglagesComptage.GENOU_HAUT else ReglagesComptage.COUDE_HAUT

        when (phase) {
            Phase.INCONNUE, Phase.HAUT -> {
                if (angle > seuilHaut) {
                    // Redescendu un peu puis remonté sans atteindre le bas : pompe pas assez profonde.
                    if (phase == Phase.HAUT && angleLePlusBas < seuilHaut - ReglagesComptage.TENTATIVE_MINIMUM) {
                        diagnostic = Diagnostic(Diagnostic.Raison.PAS_ASSEZ_BAS, angleLePlusBas, 0.0, 0.0)
                    }
                    phase = Phase.HAUT
                    noterLeHaut(mesure)
                    angleLePlusBas = angle
                } else {
                    angleLePlusBas = minOf(angleLePlusBas, angle)
                    if (angle < seuilBas && phase == Phase.HAUT) {
                        phase = Phase.BAS
                        deplacementMaximum = 0.0
                        glissementMaximum = 0.0
                        suivreLeDeplacement(mesure)
                    }
                }
            }
            Phase.BAS -> {
                angleLePlusBas = minOf(angleLePlusBas, angle)
                suivreLeDeplacement(mesure)
                if (angle > seuilHaut) {
                    phase = Phase.HAUT
                    val bouge = deplacementMaximum >= ReglagesComptage.DEPLACEMENT_MINIMUM
                    val stable = glissementMaximum <= ReglagesComptage.APPUI_MAXIMUM
                    val raison = when {
                        !bouge -> Diagnostic.Raison.CORPS_IMMOBILE
                        !stable -> Diagnostic.Raison.APPUI_DEPLACE
                        else -> Diagnostic.Raison.COMPTEE
                    }
                    diagnostic = Diagnostic(raison, angleLePlusBas, deplacementMaximum, glissementMaximum)
                    noterLeHaut(mesure)
                    angleLePlusBas = angle
                    if (bouge && stable) {
                        compte += 1
                        return true
                    }
                }
            }
        }
        return false
    }

    /** Retient, pendant la descente, le plus grand déplacement du corps et de l'appui depuis le haut. */
    private fun suivreLeDeplacement(mesure: Mesure) {
        val mobile = mobileEnHaut ?: return
        val appui = appuiEnHaut ?: return
        if (longueurEnHaut <= 0) return
        deplacementMaximum = maxOf(deplacementMaximum, Geometrie.distance(mobile, mesure.mobile) / longueurEnHaut)
        glissementMaximum = maxOf(glissementMaximum, Geometrie.distance(appui, mesure.appui) / longueurEnHaut)
    }

    private fun noterLeHaut(mesure: Mesure) {
        mobileEnHaut = mesure.mobile
        appuiEnHaut = mesure.appui
        longueurEnHaut = mesure.longueur
    }

    /**
     * Mesure l'image sur chaque côté du corps visible. Quand les deux côtés sont
     * vus (téléphone en face), on prend leur moyenne : c'est plus stable.
     */
    private fun mesurer(pose: Pose): Mesure? {
        val mesures = cotes.mapNotNull { cote ->
            if (exercice == Exercice.SQUATS) mesurerSquat(pose, cote) else mesurerPompe(pose, cote)
        }
        if (mesures.size < 2) return mesures.firstOrNull()
        val (a, b) = mesures
        return Mesure(
            angle = (a.angle + b.angle) / 2,
            mobile = milieu(a.mobile, b.mobile),
            appui = milieu(a.appui, b.appui),
            longueur = (a.longueur + b.longueur) / 2,
            postureCorrecte = a.postureCorrecte && b.postureCorrecte,
        )
    }

    private fun milieu(a: Point, b: Point) = Point((a.x + b.x) / 2, (a.y + b.y) / 2)

    private fun mesurerPompe(pose: Pose, cote: Cote): Mesure? {
        val epaule = pose[cote.epaule] ?: return null
        val coude = pose[cote.coude] ?: return null
        val poignet = pose[cote.poignet] ?: return null
        val longueur = Geometrie.distance(epaule, coude) + Geometrie.distance(coude, poignet)
        // De profil, quand la hanche et la cheville (ou le genou) sont visibles, le corps doit être droit.
        val hanche = pose[cote.hanche]
        val bas = pose[cote.cheville] ?: pose[cote.genou]
        val deProfil = bas != null && kotlin.math.abs(bas.x - epaule.x) > longueur * ReglagesComptage.PROFIL_MINIMUM
        val correcte = if (hanche != null && bas != null && deProfil) {
            Geometrie.angle(epaule, hanche, bas) >= ReglagesComptage.ALIGNEMENT_MINIMUM
        } else {
            true
        }
        return Mesure(
            angle = Geometrie.angle(epaule, coude, poignet),
            mobile = epaule,
            appui = poignet,
            longueur = longueur,
            postureCorrecte = correcte,
        )
    }

    private fun mesurerSquat(pose: Pose, cote: Cote): Mesure? {
        val hanche = pose[cote.hanche] ?: return null
        val genou = pose[cote.genou] ?: return null
        val cheville = pose[cote.cheville] ?: return null
        return Mesure(
            angle = Geometrie.angle(hanche, genou, cheville),
            mobile = hanche,
            appui = cheville,
            longueur = Geometrie.distance(hanche, genou) + Geometrie.distance(genou, cheville),
            postureCorrecte = true,
        )
    }
}

/**
 * Ce qui est arrivé à une répétition. Le contrôle du déplacement refuse de
 * compter quand on plie les bras sans descendre, ou quand on bouge seulement la tête.
 */
data class Diagnostic(
    val raison: Raison,
    /** Angle le plus fermé atteint, en degrés. */
    val angleBas: Double,
    /** Déplacement du corps, en part de la longueur du membre. */
    val deplacement: Double,
    /** Déplacement de l'appui, en part de la longueur du membre. */
    val glissement: Double,
) {
    enum class Raison { COMPTEE, PAS_ASSEZ_BAS, CORPS_IMMOBILE, APPUI_DEPLACE }
}
