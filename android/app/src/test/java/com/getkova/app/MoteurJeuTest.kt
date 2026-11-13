package com.getkova.app

import com.getkova.app.jeu.EtatJeu
import com.getkova.app.jeu.Exercice
import com.getkova.app.jeu.Plage
import com.getkova.app.jeu.Regle
import com.getkova.app.jeu.Semaine
import com.getkova.app.jeu.ajouterPlage
import com.getkova.app.jeu.appsOuvertes
import com.getkova.app.jeu.appsSousVerrou
import com.getkova.app.jeu.basculerPlage
import com.getkova.app.jeu.bilanDeLaSemaine
import com.getkova.app.jeu.contient
import com.getkova.app.jeu.definirApps
import com.getkova.app.jeu.estBloquee
import com.getkova.app.jeu.finDOuverture
import com.getkova.app.jeu.mettreAJour
import com.getkova.app.jeu.ouvrirFenetre
import com.getkova.app.jeu.plageActive
import com.getkova.app.jeu.proposerRegle
import com.getkova.app.jeu.serie
import com.getkova.app.jeu.urgencesRestantes
import com.getkova.app.jeu.utiliserUrgence
import com.getkova.app.jeu.validerSeance
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** Les règles du jeu du brief (section 6) et les critères d'acceptation 5, 6 et 7. */
class MoteurJeuTest {
    private val zone = ZoneId.of("Europe/Paris")

    /** Un instant à l'heure de Paris. Le lundi 9 novembre 2026 sert de repère. */
    private fun date(jour: Int, heure: Int, minute: Int = 0): Long =
        ZonedDateTime.of(2026, 11, jour, heure, minute, 0, 0, zone).toInstant().toEpochMilli()

    private val travail = Plage("travail", "Travail", 9 * 60, 18 * 60, setOf(1, 2, 3, 4, 5))

    @Test
    fun laSemaineCommenceLeLundi() {
        assertEquals(1, Semaine.jour(date(9, 12), zone))
        assertEquals(5, Semaine.jour(date(13, 12), zone))
        assertEquals(7, Semaine.jour(date(15, 12), zone))
        assertEquals(date(9, 0), Semaine.debut(date(15, 23), zone))
    }

    @Test
    fun laRegleParDefautEtSesBornes() {
        assertEquals(10, Regle.parDefaut.repetitions)
        assertEquals(15, Regle.parDefaut.minutes)
        assertEquals(40, Regle.parDefaut.pompesParHeure)
        val regle = Regle.creer(5, 120).changerRepetitions(-Regle.PAS).changerMinutes(Regle.PAS)
        assertEquals(Regle(5, 120), regle)
    }

    @Test
    fun lesAutresExercicesSuiventLeMemeRapport() {
        assertEquals(15, Regle.parDefaut.repetitionsDemandees(Exercice.SQUATS))
        assertEquals(12, Regle.parDefaut.repetitionsDemandees(Exercice.FENTES))
        assertEquals(25, Regle.parDefaut.repetitionsDemandees(Exercice.JUMPING_JACKS))
        assertEquals(30, Regle(20, 15).repetitionsDemandees(Exercice.SQUATS))
    }

    @Test
    fun durcirLaReglePrendEffetToutDeSuite() {
        val (etat, differe) = EtatJeu().proposerRegle(Regle(20, 15), date(9, 10))
        assertFalse(differe)
        assertEquals(20, etat.regle.repetitions)
    }

    @Test
    fun assouplirLaReglePrendEffetDans24Heures() {
        val souple = Regle(5, 15)
        val (etat, differe) = EtatJeu().proposerRegle(souple, date(9, 10))
        assertTrue(differe)
        assertEquals("La règle ne doit pas changer tout de suite.", Regle.parDefaut, etat.regle)
        assertEquals(Regle.parDefaut, etat.mettreAJour(date(10, 9, 59)).regle)
        val apres = etat.mettreAJour(date(10, 10))
        assertEquals(souple, apres.regle)
        assertNull(apres.regleEnAttente)
    }

    @Test
    fun uneSeanceCrediteLeSolde() {
        val etat = EtatJeu().validerSeance(Exercice.POMPES, date(9, 10))
        assertEquals(15, etat.soldeMinutes)
        assertEquals(1, etat.seances.size)
        assertEquals(10, etat.seances.first().repetitions)
    }

    @Test
    fun ouvrirUneFenetreUtiliseToutLeSolde() {
        val (etat, fin) = EtatJeu().validerSeance(Exercice.POMPES, date(9, 10)).ouvrirFenetre(date(9, 10), zone)
        assertEquals(date(9, 10, 15), fin)
        assertEquals(0, etat.soldeMinutes)
        assertTrue(etat.appsOuvertes(date(9, 10, 14), zone))
        assertFalse("Les apps se reverrouillent à la fin.", etat.appsOuvertes(date(9, 10, 15), zone))
    }

    @Test
    fun sansSoldeRienNeSOuvre() {
        val (etat, fin) = EtatJeu().ouvrirFenetre(date(9, 10), zone)
        assertNull(fin)
        assertFalse(etat.appsOuvertes(date(9, 10), zone))
    }

    @Test
    fun unePlageStricteBloqueMemeAvecUneFenetre() {
        val (etat, _) = EtatJeu().ajouterPlage(travail)
            .validerSeance(Exercice.POMPES, date(9, 8, 50))
            .ouvrirFenetre(date(9, 8, 50), zone)
        assertTrue(etat.appsOuvertes(date(9, 8, 55), zone))
        assertFalse("La plage reverrouille pendant la fenêtre.", etat.appsOuvertes(date(9, 9, 1), zone))
        assertTrue(etat.plageActive(date(9, 12), zone))
        assertFalse("Le samedi, la plage de semaine ne s'applique pas.", etat.plageActive(date(14, 12), zone))
        assertEquals("L'heure de fin affichée s'arrête au début de la plage.", date(9, 9), etat.finDOuverture(date(9, 8, 55), zone))
    }

    @Test
    fun pendantUnePlageOnNePeutPasOuvrirDeFenetre() {
        val depart = EtatJeu().ajouterPlage(travail).validerSeance(Exercice.POMPES, date(9, 10))
        val (etat, fin) = depart.ouvrirFenetre(date(9, 10), zone)
        assertNull(fin)
        assertEquals("Le solde reste intact.", 15, etat.soldeMinutes)
    }

    @Test
    fun unePlageQuiPasseMinuit() {
        val nuit = Plage("nuit", "Nuit", 23 * 60, 7 * 60, setOf(1))
        assertTrue("Lundi soir.", nuit.contient(date(9, 23, 30), zone))
        assertTrue("Mardi matin, suite de la nuit de lundi.", nuit.contient(date(10, 6), zone))
        assertFalse(nuit.contient(date(10, 7), zone))
        assertFalse("Lundi matin : la plage n'a pas commencé dimanche.", nuit.contient(date(9, 6), zone))
    }

    @Test
    fun desactiverUnePlagePrendEffetDans24Heures() {
        val plage = travail.copy(jours = Plage.tousLesJours)
        val etat = EtatJeu().ajouterPlage(plage).basculerPlage(plage.id, date(9, 10))
        assertTrue("Encore en vigueur le jour même.", etat.plageActive(date(9, 17), zone))
        assertTrue("Encore en vigueur 23 h 30 plus tard.", etat.plageActive(date(10, 9, 30), zone))
        assertFalse("Désactivée après 24 h.", etat.plageActive(date(10, 10, 1), zone))
        assertTrue("Réactiver est immédiat.", etat.basculerPlage(plage.id, date(10, 11)).plageActive(date(10, 11), zone))
    }

    @Test
    fun lUrgenceMarcheDeuxFoisParSemainePasTrois() {
        var etat = EtatJeu()
        assertEquals(2, etat.urgencesRestantes(date(9, 10), zone))
        val premiere = etat.utiliserUrgence(date(9, 10), zone)
        assertEquals(date(9, 10, 5), premiere.second)
        etat = premiere.first
        val deuxieme = etat.utiliserUrgence(date(11, 10), zone)
        assertNotNull(deuxieme.second)
        etat = deuxieme.first
        assertNull("La troisième est refusée.", etat.utiliserUrgence(date(13, 10), zone).second)
        assertEquals(0, etat.urgencesRestantes(date(13, 10), zone))
    }

    @Test
    fun lUrgenceRevientLeLundi() {
        val etat = EtatJeu().utiliserUrgence(date(9, 10), zone).first.utiliserUrgence(date(15, 22), zone).first
        assertEquals(0, etat.urgencesRestantes(date(15, 23), zone))
        assertEquals(2, etat.urgencesRestantes(date(16, 0, 1), zone))
    }

    @Test
    fun lUrgencePasseMemePendantUnePlage() {
        val plage = travail.copy(jours = Plage.tousLesJours)
        val etat = EtatJeu().ajouterPlage(plage).utiliserUrgence(date(9, 10), zone).first
        assertTrue(etat.appsOuvertes(date(9, 10, 4), zone))
        assertFalse(etat.appsOuvertes(date(9, 10, 5), zone))
    }

    @Test
    fun lUrgenceDesactiveeNeMarchePas() {
        assertNull(EtatJeu(urgenceActivee = false).utiliserUrgence(date(9, 10), zone).second)
    }

    @Test
    fun retirerUneAppPrendEffetDans24Heures() {
        val depart = EtatJeu().definirApps(setOf("a", "b"), date(9, 10), immediat = true)
        val etat = depart.definirApps(setOf("a"), date(9, 12), immediat = false)
        assertEquals(setOf("a"), etat.apps)
        assertTrue("Encore bloquée le jour même.", etat.estBloquee("b", date(9, 20), zone))
        assertTrue("Encore bloquée 23 h plus tard.", etat.estBloquee("b", date(10, 11), zone))
        assertFalse("Libérée après 24 h.", etat.estBloquee("b", date(10, 12, 1), zone))
        assertEquals(setOf("a"), etat.mettreAJour(date(10, 13)).appsSousVerrou(date(10, 13)))
    }

    @Test
    fun ajouterUneAppEstImmediat() {
        val etat = EtatJeu().definirApps(setOf("a"), date(9, 10), immediat = true)
            .definirApps(setOf("a", "c"), date(9, 12), immediat = false)
        assertTrue(etat.estBloquee("c", date(9, 12), zone))
        assertFalse("Une app jamais choisie n'est pas bloquée.", etat.estBloquee("z", date(9, 12), zone))
    }

    @Test
    fun laSerieCompteLesJoursDAffilee() {
        var etat = EtatJeu()
        assertEquals(0, etat.serie(date(13, 10), zone))
        etat = etat.validerSeance(Exercice.POMPES, date(10, 10))
            .validerSeance(Exercice.POMPES, date(11, 10))
            .validerSeance(Exercice.POMPES, date(12, 10))
        assertEquals(3, etat.serie(date(12, 20), zone))
        assertEquals("La série tient jusqu'au lendemain soir.", 3, etat.serie(date(13, 8), zone))
        assertEquals("Un jour sans séance casse la série.", 0, etat.serie(date(14, 8), zone))
    }

    @Test
    fun leBilanDeLaSemaine() {
        val etat = EtatJeu()
            .validerSeance(Exercice.POMPES, date(3, 10))
            .validerSeance(Exercice.POMPES, date(9, 10))
            .validerSeance(Exercice.SQUATS, date(9, 18))
            .validerSeance(Exercice.POMPES, date(13, 10))
        val bilan = etat.bilanDeLaSemaine(date(13, 12), zone)
        assertEquals(listOf(25, 0, 0, 0, 10, 0, 0), bilan.repetitionsParJour)
        assertEquals(4, bilan.rangAujourdHui)
        assertEquals(3, bilan.seances)
        assertEquals(45, bilan.minutesGagnees)
        assertEquals(250, bilan.progressionPourcent)
    }

    @Test
    fun lEtatSurvitALEnregistrement() {
        val etat = EtatJeu()
            .validerSeance(Exercice.POMPES, date(9, 10))
            .ajouterPlage(Plage("nuit", "Nuit", 23 * 60, 7 * 60))
            .definirApps(setOf("a"), date(9, 10), immediat = true)
            .proposerRegle(Regle(5, 15), date(9, 10)).first
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(etat, json.decodeFromString<EtatJeu>(json.encodeToString(EtatJeu.serializer(), etat)))
        // Un enregistrement ancien, sans les champs récents, garde ses valeurs.
        assertEquals(7, json.decodeFromString<EtatJeu>("""{"soldeMinutes":7,"champInconnu":1}""").soldeMinutes)
    }
}
