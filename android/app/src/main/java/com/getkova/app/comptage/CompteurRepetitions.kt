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
    /** Pompe : bas quand le coude passe sous 90°, haut quand il repasse au-dessus de 160°. */
    const val COUDE_BAS = 90.0
    const val COUDE_HAUT = 160.0

    /** Squat : bas quand le genou passe sous 100°, haut quand il repasse au-dessus de 160°. */
    const val GENOU_BAS = 100.0
    const val GENOU_HAUT = 160.0

    /** Le corps doit rester droit : angle épaule, hanche, cheville au minimum. */
    const val ALIGNEMENT_MINIMUM = 140.0

    /** Entre le haut et le bas, l'épaule (pompe) ou la hanche (squat) doit bouger d'au moins cette part du membre. */
    const val DEPLACEMENT_MINIMUM = 0.25

    /** Et l'appui (poignet ou cheville) doit rester presque immobile. */
    const val APPUI_MAXIMUM = 0.25

    /** Nombre d'images sur lesquelles l'angle est lissé. */
    const val LISSAGE = 5

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

    private var phase = Phase.INCONNUE
    private val angles = ArrayDeque<Double>()
    private var imagesSansCorps = 0
    private var mobileEnHaut: Point? = null
    private var appuiEnHaut: Point? = null
    private var longueurEnHaut = 0.0
    private var basValide = false

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

        val squat = exercice == Exercice.SQUATS
        val seuilBas = if (squat) ReglagesComptage.GENOU_BAS else ReglagesComptage.COUDE_BAS
        val seuilHaut = if (squat) ReglagesComptage.GENOU_HAUT else ReglagesComptage.COUDE_HAUT

        when (phase) {
            Phase.INCONNUE, Phase.HAUT -> {
                if (angle > seuilHaut) {
                    phase = Phase.HAUT
                    noterLeHaut(mesure)
                } else if (angle < seuilBas && phase == Phase.HAUT) {
                    phase = Phase.BAS
                    basValide = deplacementCorrect(mesure)
                }
            }
            Phase.BAS -> {
                // Le déplacement peut être confirmé plus bas dans la descente.
                if (!basValide) basValide = deplacementCorrect(mesure)
                if (angle > seuilHaut) {
                    phase = Phase.HAUT
                    val valide = basValide
                    basValide = false
                    noterLeHaut(mesure)
                    if (valide) {
                        compte += 1
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun noterLeHaut(mesure: Mesure) {
        mobileEnHaut = mesure.mobile
        appuiEnHaut = mesure.appui
        longueurEnHaut = mesure.longueur
    }

    /** Choisit le côté du corps le mieux vu et en tire la mesure de l'image. */
    private fun mesurer(pose: Pose): Mesure? = cotes.firstNotNullOfOrNull { cote ->
        if (exercice == Exercice.SQUATS) mesurerSquat(pose, cote) else mesurerPompe(pose, cote)
    }

    private fun mesurerPompe(pose: Pose, cote: Cote): Mesure? {
        val epaule = pose[cote.epaule] ?: return null
        val coude = pose[cote.coude] ?: return null
        val poignet = pose[cote.poignet] ?: return null
        // Quand la hanche et la cheville (ou le genou) sont visibles, le corps doit être droit.
        val hanche = pose[cote.hanche]
        val bas = pose[cote.cheville] ?: pose[cote.genou]
        val correcte = if (hanche != null && bas != null) {
            Geometrie.angle(epaule, hanche, bas) >= ReglagesComptage.ALIGNEMENT_MINIMUM
        } else {
            true
        }
        return Mesure(
            angle = Geometrie.angle(epaule, coude, poignet),
            mobile = epaule,
            appui = poignet,
            longueur = Geometrie.distance(epaule, coude) + Geometrie.distance(coude, poignet),
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

    /**
     * Vrai si, depuis le haut, le corps est vraiment descendu : le point mobile a
     * bougé et l'appui est resté en place. Plier les bras debout, ou bouger
     * seulement la tête, ne passe pas ce contrôle.
     */
    private fun deplacementCorrect(mesure: Mesure): Boolean {
        val mobile = mobileEnHaut ?: return false
        val appui = appuiEnHaut ?: return false
        if (longueurEnHaut <= 0) return false
        val deplacement = Geometrie.distance(mobile, mesure.mobile) / longueurEnHaut
        val glissement = Geometrie.distance(appui, mesure.appui) / longueurEnHaut
        return deplacement >= ReglagesComptage.DEPLACEMENT_MINIMUM && glissement <= ReglagesComptage.APPUI_MAXIMUM
    }
}
