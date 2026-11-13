import XCTest
@testable import Kova

/// Les règles du jeu du brief (section 6) et les critères d'acceptation 5, 6 et 7.
final class MoteurJeuTests: XCTestCase {
    private let calendrier = Semaine.calendrier(fuseau: TimeZone(identifier: "Europe/Paris") ?? .current)

    /// Une date à l'heure de Paris. Le lundi 9 novembre 2026 sert de repère.
    private func date(_ jour: Int, _ heure: Int, _ minute: Int = 0) -> Date {
        var composants = DateComponents()
        composants.year = 2026
        composants.month = 11
        composants.day = jour
        composants.hour = heure
        composants.minute = minute
        return calendrier.date(from: composants) ?? Date()
    }

    // MARK: Semaine

    func testLaSemaineCommenceLeLundi() {
        XCTAssertEqual(Semaine.jour(date(9, 12), calendrier), 1)
        XCTAssertEqual(Semaine.jour(date(13, 12), calendrier), 5)
        XCTAssertEqual(Semaine.jour(date(15, 12), calendrier), 7)
        XCTAssertEqual(Semaine.debut(date(15, 23), calendrier), date(9, 0))
    }

    // MARK: Règle

    func testLaRegleParDefautEtSesBornes() {
        XCTAssertEqual(Regle.parDefaut.repetitions, 10)
        XCTAssertEqual(Regle.parDefaut.minutes, 15)
        XCTAssertEqual(Regle.parDefaut.pompesParHeure, 40)
        var regle = Regle(repetitions: 5, minutes: 120)
        regle.changerRepetitions(de: -Regle.pas)
        regle.changerMinutes(de: Regle.pas)
        XCTAssertEqual(regle.repetitions, 5)
        XCTAssertEqual(regle.minutes, 120)
    }

    func testLesAutresExercicesSuiventLeMemeRapport() {
        XCTAssertEqual(Regle.parDefaut.repetitionsDemandees(pour: .squats), 15)
        XCTAssertEqual(Regle.parDefaut.repetitionsDemandees(pour: .fentes), 12)
        XCTAssertEqual(Regle.parDefaut.repetitionsDemandees(pour: .jumpingJacks), 25)
        let double = Regle(repetitions: 20, minutes: 15)
        XCTAssertEqual(double.repetitionsDemandees(pour: .squats), 30)
    }

    func testDurcirLaReglePrendEffetToutDeSuite() {
        var etat = EtatJeu()
        let differe = etat.proposerRegle(Regle(repetitions: 20, minutes: 15), date(9, 10))
        XCTAssertFalse(differe)
        XCTAssertEqual(etat.regle.repetitions, 20)
    }

    func testAssouplirLaReglePrendEffetDans24Heures() {
        var etat = EtatJeu()
        let souple = Regle(repetitions: 5, minutes: 15)
        XCTAssertTrue(etat.proposerRegle(souple, date(9, 10)))
        XCTAssertEqual(etat.regle, Regle.parDefaut, "La règle ne doit pas changer tout de suite.")

        etat.mettreAJour(date(10, 9, 59))
        XCTAssertEqual(etat.regle, Regle.parDefaut)

        etat.mettreAJour(date(10, 10))
        XCTAssertEqual(etat.regle, souple)
        XCTAssertNil(etat.regleEnAttente)
    }

    // MARK: Séance, solde et fenêtre

    func testUneSeanceCrediteLeSolde() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10))
        XCTAssertEqual(etat.soldeMinutes, 15)
        XCTAssertEqual(etat.seances.count, 1)
        XCTAssertEqual(etat.seances.first?.repetitions, 10)
    }

    func testOuvrirUneFenetreUtiliseToutLeSolde() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10))
        let fin = etat.ouvrirFenetre(date(9, 10), calendrier)
        XCTAssertEqual(fin, date(9, 10, 15))
        XCTAssertEqual(etat.soldeMinutes, 0)
        XCTAssertTrue(etat.appsOuvertes(date(9, 10, 14), calendrier))
        XCTAssertFalse(etat.appsOuvertes(date(9, 10, 15), calendrier), "Les apps se reverrouillent à la fin.")
    }

    func testSansSoldeRienNeSOuvre() {
        var etat = EtatJeu()
        XCTAssertNil(etat.ouvrirFenetre(date(9, 10), calendrier))
        XCTAssertFalse(etat.appsOuvertes(date(9, 10), calendrier))
    }

    // MARK: Planning (critère 5)

    func testUnePlageStricteBloqueMemeAvecUneFenetre() {
        var etat = EtatJeu()
        etat.ajouterPlage(Plage(nom: "Travail", debut: 9 * 60, fin: 18 * 60, jours: [1, 2, 3, 4, 5]))
        etat.validerSeance(.pompes, date(9, 8, 50))
        etat.ouvrirFenetre(date(9, 8, 50), calendrier)

        XCTAssertTrue(etat.appsOuvertes(date(9, 8, 55), calendrier))
        XCTAssertFalse(etat.appsOuvertes(date(9, 9, 1), calendrier), "La plage reverrouille pendant la fenêtre.")
        XCTAssertTrue(etat.plageActive(date(9, 12), calendrier))
        XCTAssertFalse(etat.plageActive(date(14, 12), calendrier), "Le samedi, la plage de semaine ne s'applique pas.")
    }

    func testPendantUnePlageOnNePeutPasOuvrirDeFenetre() {
        var etat = EtatJeu()
        etat.ajouterPlage(Plage(nom: "Travail", debut: 9 * 60, fin: 18 * 60, jours: Plage.tousLesJours))
        etat.validerSeance(.pompes, date(9, 10))
        XCTAssertNil(etat.ouvrirFenetre(date(9, 10), calendrier))
        XCTAssertEqual(etat.soldeMinutes, 15, "Le solde reste intact.")
    }

    func testUnePlageQuiPasseMinuit() {
        let nuit = Plage(nom: "Nuit", debut: 23 * 60, fin: 7 * 60, jours: [1])
        XCTAssertTrue(nuit.contient(date(9, 23, 30), calendrier), "Lundi soir.")
        XCTAssertTrue(nuit.contient(date(10, 6), calendrier), "Mardi matin, suite de la nuit de lundi.")
        XCTAssertFalse(nuit.contient(date(10, 7), calendrier))
        XCTAssertFalse(nuit.contient(date(9, 6), calendrier), "Lundi matin : la plage n'a pas commencé dimanche.")
    }

    func testDesactiverUnePlagePrendEffetDans24Heures() {
        var etat = EtatJeu()
        let plage = Plage(nom: "Travail", debut: 9 * 60, fin: 18 * 60, jours: Plage.tousLesJours)
        etat.ajouterPlage(plage)
        etat.basculerPlage(plage.id, date(9, 10))

        XCTAssertTrue(etat.plageActive(date(9, 17), calendrier), "Encore en vigueur le jour même.")
        XCTAssertTrue(etat.plageActive(date(10, 9, 30), calendrier), "Encore en vigueur 23 h 30 plus tard.")
        XCTAssertFalse(etat.plageActive(date(10, 10, 1), calendrier), "Désactivée après 24 h.")

        etat.basculerPlage(plage.id, date(10, 11))
        XCTAssertTrue(etat.plageActive(date(10, 11), calendrier), "Réactiver est immédiat.")
    }

    // MARK: Urgence (critère 6)

    func testLUrgenceMarcheDeuxFoisParSemainePasTrois() {
        var etat = EtatJeu()
        XCTAssertEqual(etat.urgencesRestantes(date(9, 10), calendrier), 2)
        XCTAssertEqual(etat.utiliserUrgence(date(9, 10), calendrier), date(9, 10, 5))
        XCTAssertNotNil(etat.utiliserUrgence(date(11, 10), calendrier))
        XCTAssertNil(etat.utiliserUrgence(date(13, 10), calendrier), "La troisième est refusée.")
        XCTAssertEqual(etat.urgencesRestantes(date(13, 10), calendrier), 0)
    }

    func testLUrgenceRevientLeLundi() {
        var etat = EtatJeu()
        etat.utiliserUrgence(date(9, 10), calendrier)
        etat.utiliserUrgence(date(15, 22), calendrier)
        XCTAssertEqual(etat.urgencesRestantes(date(15, 23), calendrier), 0)
        XCTAssertEqual(etat.urgencesRestantes(date(16, 0, 1), calendrier), 2)
    }

    func testLUrgencePasseMemePendantUnePlage() {
        var etat = EtatJeu()
        etat.ajouterPlage(Plage(nom: "Travail", debut: 9 * 60, fin: 18 * 60, jours: Plage.tousLesJours))
        etat.utiliserUrgence(date(9, 10), calendrier)
        XCTAssertTrue(etat.appsOuvertes(date(9, 10, 4), calendrier))
        XCTAssertFalse(etat.appsOuvertes(date(9, 10, 5), calendrier))
    }

    func testLUrgenceDesactiveeNeMarchePas() {
        var etat = EtatJeu()
        etat.urgenceActivee = false
        XCTAssertNil(etat.utiliserUrgence(date(9, 10), calendrier))
    }

    // MARK: Série et stats

    func testLaSerieCompteLesJoursDAffilee() {
        var etat = EtatJeu()
        XCTAssertEqual(etat.serie(date(13, 10), calendrier), 0)
        etat.validerSeance(.pompes, date(10, 10))
        etat.validerSeance(.pompes, date(11, 10))
        etat.validerSeance(.pompes, date(12, 10))
        XCTAssertEqual(etat.serie(date(12, 20), calendrier), 3)
        XCTAssertEqual(etat.serie(date(13, 8), calendrier), 3, "La série tient jusqu'au lendemain soir.")
        XCTAssertEqual(etat.serie(date(14, 8), calendrier), 0, "Un jour sans séance casse la série.")
    }

    func testLeBilanDeLaSemaine() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(3, 10))
        etat.validerSeance(.pompes, date(9, 10))
        etat.validerSeance(.squats, date(9, 18))
        etat.validerSeance(.pompes, date(13, 10))
        let bilan = etat.bilanDeLaSemaine(date(13, 12), calendrier)
        XCTAssertEqual(bilan.repetitionsParJour, [25, 0, 0, 0, 10, 0, 0])
        XCTAssertEqual(bilan.rangAujourdHui, 4)
        XCTAssertEqual(bilan.seances, 3)
        XCTAssertEqual(bilan.minutesGagnees, 45)
        XCTAssertEqual(bilan.progressionPourcent, 250)
    }

    // MARK: Enregistrement

    func testLEtatSurvitALEnregistrement() throws {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10))
        etat.ajouterPlage(Plage(nom: "Nuit", debut: 23 * 60, fin: 7 * 60, jours: Plage.tousLesJours))
        etat.proposerRegle(Regle(repetitions: 5, minutes: 15), date(9, 10))
        let donnees = try JSONEncoder().encode(etat)
        XCTAssertEqual(try JSONDecoder().decode(EtatJeu.self, from: donnees), etat)
    }
}
