package com.getkova.app

import com.getkova.app.comptage.Articulation
import com.getkova.app.comptage.CompteurRepetitions
import com.getkova.app.comptage.Diagnostic
import com.getkova.app.comptage.Geometrie
import com.getkova.app.comptage.Point
import com.getkova.app.comptage.Pose
import com.getkova.app.comptage.ReglagesComptage
import com.getkova.app.jeu.Exercice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Critère d'acceptation 3 du brief, sur des mouvements dessinés à la main :
 * 10 pompes correctes sont comptées 10, les faux mouvements sont comptés 0.
 * Ces tests valident la logique. Seul un essai sur un vrai téléphone valide
 * le comptage face à une vraie caméra.
 */
class CompteurRepetitionsTest {
    // Pompe vue de côté : poignet fixe au sol, épaule qui descend.
    private val haut: Pose = mapOf(
        Articulation.EPAULE_GAUCHE to Point(0.5, 0.5), Articulation.COUDE_GAUCHE to Point(0.5, 0.3),
        Articulation.POIGNET_GAUCHE to Point(0.5, 0.1), Articulation.HANCHE_GAUCHE to Point(0.9, 0.4),
        Articulation.CHEVILLE_GAUCHE to Point(1.3, 0.3),
    )
    private val bas: Pose = mapOf(
        Articulation.EPAULE_GAUCHE to Point(0.5, 0.22), Articulation.COUDE_GAUCHE to Point(0.62, 0.16),
        Articulation.POIGNET_GAUCHE to Point(0.5, 0.1), Articulation.HANCHE_GAUCHE to Point(0.9, 0.21),
        Articulation.CHEVILLE_GAUCHE to Point(1.3, 0.2),
    )

    /** Posture intermédiaire : 0 = en haut, 1 = en bas. */
    private fun melange(a: Pose, b: Pose, part: Double): Pose = a.mapValues { (articulation, depart) ->
        val arrivee = b[articulation] ?: depart
        Point(depart.x + (arrivee.x - depart.x) * part, depart.y + (arrivee.y - depart.y) * part)
    }

    /** Une répétition : descente, pause, remontée, pause. */
    private fun repetition(a: Pose, b: Pose, amplitude: Double = 1.0): List<Pose?> {
        val pas = 12
        return (0..pas).map { melange(a, b, amplitude * it / pas) } +
            List(6) { melange(a, b, amplitude) } +
            (0..pas).map { melange(a, b, amplitude * (1 - it.toDouble() / pas)) } +
            List(6) { melange(a, b, 0.0) }
    }

    private fun dixRepetitions(a: Pose, b: Pose, amplitude: Double = 1.0): List<Pose?> =
        List(8) { melange(a, b, 0.0) } + (1..10).flatMap { repetition(a, b, amplitude) }

    private fun compter(exercice: Exercice, images: List<Pose?>): CompteurRepetitions {
        val compteur = CompteurRepetitions(exercice)
        images.forEach { compteur.traiter(it) }
        return compteur
    }

    @Test
    fun dixPompesCorrectesSontCompteesDix() {
        assertEquals(10, compter(Exercice.POMPES, dixRepetitions(haut, bas)).compte)
    }

    @Test
    fun dixPompesRapidesSontCompteesDix() {
        // Peu d'images par pompe, sans pause : le lissage ne doit pas écraser le mouvement.
        val pas = 4
        val rapide = (0..pas).map { melange(haut, bas, it.toDouble() / pas) } +
            (1..pas).map { melange(haut, bas, 1 - it.toDouble() / pas) }
        val images = List(5) { melange(haut, bas, 0.0) } + (1..10).flatMap { rapide } + List(3) { melange(haut, bas, 0.0) }
        assertEquals(10, compter(Exercice.POMPES, images).compte)
    }

    @Test
    fun lePourquoiDUnRefusEstConnu() {
        // Une descente franche mais incomplète : le coude s'arrête vers 116°.
        val demi = compter(Exercice.POMPES, dixRepetitions(haut, bas, amplitude = 0.6))
        assertEquals(0, demi.compte)
        assertEquals(Diagnostic.Raison.PAS_ASSEZ_BAS, demi.diagnostic?.raison)
        val correcte = compter(Exercice.POMPES, dixRepetitions(haut, bas))
        assertEquals(Diagnostic.Raison.COMPTEE, correcte.diagnostic?.raison)
    }

    @Test
    fun bougerSeulementLaTeteNeCompteRien() {
        // Le corps ne bouge pas : aucune articulation suivie ne change.
        assertEquals(0, compter(Exercice.POMPES, List(400) { haut }).compte)
    }

    @Test
    fun plierLesBrasDeboutNeCompteRien() {
        // Épaule immobile, poignet qui remonte : le coude plie, mais le corps ne descend pas.
        val brasTendu: Pose = mapOf(
            Articulation.EPAULE_GAUCHE to Point(0.5, 0.6), Articulation.COUDE_GAUCHE to Point(0.5, 0.4),
            Articulation.POIGNET_GAUCHE to Point(0.5, 0.2),
        )
        val brasPlie: Pose = mapOf(
            Articulation.EPAULE_GAUCHE to Point(0.5, 0.6), Articulation.COUDE_GAUCHE to Point(0.5, 0.4),
            Articulation.POIGNET_GAUCHE to Point(0.62, 0.55),
        )
        assertEquals(0, compter(Exercice.POMPES, dixRepetitions(brasTendu, brasPlie)).compte)
    }

    @Test
    fun demiPompesNeComptentPas() {
        assertEquals(0, compter(Exercice.POMPES, dixRepetitions(haut, bas, amplitude = 0.4)).compte)
    }

    @Test
    fun corpsHorsCadreMetEnPause() {
        val compteur = compter(Exercice.POMPES, List(8) { haut } + repetition(haut, bas))
        assertTrue(compteur.corpsVisible)
        repeat(ReglagesComptage.IMAGES_AVANT_PAUSE) { compteur.traiter(null) }
        assertFalse(compteur.corpsVisible)
        assertEquals("Sortir du cadre ne doit pas effacer les répétitions déjà comptées.", 1, compteur.compte)
    }

    @Test
    fun dixSquatsSontComptesDix() {
        // Squat vu de côté : cheville fixe, hanche qui descend, genou qui plie.
        val debout: Pose = mapOf(
            Articulation.HANCHE_GAUCHE to Point(0.5, 0.6), Articulation.GENOU_GAUCHE to Point(0.5, 0.35),
            Articulation.CHEVILLE_GAUCHE to Point(0.5, 0.1),
        )
        val accroupi: Pose = mapOf(
            Articulation.HANCHE_GAUCHE to Point(0.36, 0.32), Articulation.GENOU_GAUCHE to Point(0.62, 0.32),
            Articulation.CHEVILLE_GAUCHE to Point(0.5, 0.1),
        )
        assertEquals(10, compter(Exercice.SQUATS, dixRepetitions(debout, accroupi)).compte)
    }

    @Test
    fun angleDroit() {
        assertEquals(90.0, Geometrie.angle(Point(0.0, 1.0), Point(0.0, 0.0), Point(1.0, 0.0)), 0.001)
    }
}
