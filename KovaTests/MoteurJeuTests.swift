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
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        XCTAssertEqual(etat.soldeMinutes, 15)
        XCTAssertEqual(etat.seances.count, 1)
        XCTAssertEqual(etat.seances.first?.repetitions, 10)
    }

    func testOuvrirUneFenetreUtiliseToutLeSolde() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier)
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
        etat.validerSeance(.pompes, date(9, 8, 50), calendrier)
        etat.ouvrirFenetre(date(9, 8, 50), calendrier)

        XCTAssertTrue(etat.appsOuvertes(date(9, 8, 55), calendrier))
        XCTAssertFalse(etat.appsOuvertes(date(9, 9, 1), calendrier), "La plage reverrouille pendant la fenêtre.")
        XCTAssertTrue(etat.plageActive(date(9, 12), calendrier))
        XCTAssertFalse(etat.plageActive(date(14, 12), calendrier), "Le samedi, la plage de semaine ne s'applique pas.")
        XCTAssertEqual(
            etat.finDOuverture(date(9, 8, 55), calendrier),
            date(9, 9),
            "L'heure de fin affichée s'arrête au début de la plage."
        )
    }

    func testPendantUnePlageOnNePeutPasOuvrirDeFenetre() {
        var etat = EtatJeu()
        etat.ajouterPlage(Plage(nom: "Travail", debut: 9 * 60, fin: 18 * 60, jours: Plage.tousLesJours))
        etat.validerSeance(.pompes, date(9, 10), calendrier)
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
        etat.validerSeance(.pompes, date(10, 10), calendrier)
        etat.validerSeance(.pompes, date(11, 10), calendrier)
        etat.validerSeance(.pompes, date(12, 10), calendrier)
        XCTAssertEqual(etat.serie(date(12, 20), calendrier), 3)
        XCTAssertEqual(etat.serie(date(13, 8), calendrier), 3, "La série tient jusqu'au lendemain soir.")
        XCTAssertEqual(etat.serie(date(14, 8), calendrier), 3, "Le joker de la semaine pardonne un jour raté.")
        XCTAssertEqual(etat.serie(date(15, 8), calendrier), 0, "Deux jours ratés la même semaine cassent la série.")
    }

    func testLeJokerRelieDeuxJoursAvecSeance() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.validerSeance(.pompes, date(10, 10), calendrier)
        etat.validerSeance(.pompes, date(12, 10), calendrier)
        // Le mercredi 11 est raté : il ne compte pas, mais la série continue.
        XCTAssertEqual(etat.serie(date(12, 20), calendrier), 3)
        XCTAssertTrue(etat.jokerUtilise(date(12, 20), calendrier))
        // La semaine suivante, le joker est de nouveau disponible.
        etat.validerSeance(.pompes, date(13, 10), calendrier)
        XCTAssertFalse(etat.jokerUtilise(date(16, 9), calendrier))
    }

    func testUnJokerParSemaineSeulement() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.validerSeance(.pompes, date(11, 10), calendrier)
        etat.validerSeance(.pompes, date(13, 10), calendrier)
        // Mardi 10 et jeudi 12 ratés la même semaine : seul le jeudi est pardonné.
        XCTAssertEqual(etat.serie(date(13, 20), calendrier), 2)
    }

    func testSansSeanceAvantLeJokerNeSauveRien() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(11, 10), calendrier)
        XCTAssertEqual(etat.serie(date(11, 20), calendrier), 1)
        XCTAssertFalse(etat.jokerUtilise(date(11, 20), calendrier))
    }

    func testLeRecordSeCompareAuxSeancesDuMemeExercice() {
        var premiere = EtatJeu()
        premiere.validerSeance(.pompes, date(9, 10), calendrier, repetitions: 20)
        XCTAssertFalse(premiere.estUnRecord(), "La première séance n'est pas un record.")

        var egale = premiere
        egale.validerSeance(.pompes, date(10, 10), calendrier, repetitions: 20)
        XCTAssertFalse(egale.estUnRecord())

        var meilleure = premiere
        meilleure.validerSeance(.pompes, date(10, 10), calendrier, repetitions: 21)
        XCTAssertTrue(meilleure.estUnRecord())

        var autreExercice = premiere
        autreExercice.validerSeance(.squats, date(10, 10), calendrier, repetitions: 40)
        XCTAssertFalse(autreExercice.estUnRecord())
    }

    func testLeBilanDeLaSemaine() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(3, 10), calendrier)
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.validerSeance(.squats, date(9, 18), calendrier)
        etat.validerSeance(.pompes, date(13, 10), calendrier)
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
        etat.definirHardcore(true, date(9, 9), calendrier, immediat: false)
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.ajouterPlage(Plage(nom: "Nuit", debut: 23 * 60, fin: 7 * 60, jours: Plage.tousLesJours))
        etat.proposerRegle(Regle(repetitions: 5, minutes: 15), date(9, 10))
        etat.definirHardcore(false, date(9, 11), calendrier, immediat: false)
        let donnees = try JSONEncoder().encode(etat)
        XCTAssertEqual(try JSONDecoder().decode(EtatJeu.self, from: donnees), etat)
        // Un enregistrement ancien, sans les champs récents, garde ses valeurs.
        let ancien = Data("{\"soldeMinutes\":7,\"champInconnu\":1}".utf8)
        let relu = try JSONDecoder().decode(EtatJeu.self, from: ancien)
        XCTAssertEqual(relu.soldeMinutes, 7)
        XCTAssertEqual(relu.soldeSecondes, 0)
        XCTAssertFalse(relu.hardcore)
        XCTAssertNil(relu.hardcoreJusquA)
        XCTAssertNil(relu.effacementLe)
    }

    // MARK: Mode hardcore

    private func hardcore(_ instant: Date) -> EtatJeu {
        var etat = EtatJeu()
        etat.definirHardcore(true, instant, calendrier, immediat: false)
        return etat
    }

    func testSansHardcoreLeSoldeResteLeLendemain() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.mettreAJour(date(10, 8))
        XCTAssertEqual(etat.soldeMinutes, 15)
        XCTAssertNil(etat.effacementLe)
    }

    func testEnHardcoreLeSoldeSEffaceAMinuit() {
        var etat = hardcore(date(9, 9))
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        XCTAssertEqual(etat.effacementLe, date(10, 0))

        var avant = etat
        avant.mettreAJour(date(9, 23, 59))
        XCTAssertEqual(avant.soldeMinutes, 15)

        var lendemain = etat
        lendemain.mettreAJour(date(10, 0))
        XCTAssertEqual(lendemain.soldeMinutes, 0)
        XCTAssertNil(lendemain.effacementLe)
        // Les séances restent : la série et les stats ne sont pas touchées.
        XCTAssertEqual(lendemain.seances.count, 1)
    }

    func testEnHardcoreLaFenetreSArreteAMinuit() {
        var etat = hardcore(date(9, 9))
        etat.validerSeance(.pompes, date(9, 23, 50), calendrier)
        let fin = etat.ouvrirFenetre(date(9, 23, 50), calendrier)
        XCTAssertEqual(fin, date(10, 0))
        XCTAssertEqual(etat.finDOuverture(date(9, 23, 55), calendrier), date(10, 0))
        etat.mettreAJour(date(10, 0))
        XCTAssertFalse(etat.appsOuvertes(date(10, 0), calendrier))
    }

    func testActiverLeHardcoreBorneLeTempsDejaGagne() {
        var etat = EtatJeu()
        etat.regle = Regle(repetitions: 10, minutes: 120)
        etat.validerSeance(.pompes, date(9, 23), calendrier)
        etat.ouvrirFenetre(date(9, 23), calendrier)
        etat.validerSeance(.pompes, date(9, 23), calendrier)
        etat.definirHardcore(true, date(9, 23, 10), calendrier, immediat: false)
        XCTAssertEqual(etat.fenetreFin, date(10, 0))
        XCTAssertEqual(etat.soldeMinutes, 120)
        etat.mettreAJour(date(10, 0, 1))
        XCTAssertEqual(etat.soldeMinutes, 0)
    }

    func testCouperLeHardcorePrend24Heures() {
        var coupe = hardcore(date(9, 9))
        coupe.definirHardcore(false, date(9, 20), calendrier, immediat: false)
        XCTAssertFalse(coupe.hardcore)
        XCTAssertTrue(coupe.hardcoreEnVigueur(date(10, 19, 59)))
        XCTAssertFalse(coupe.hardcoreEnVigueur(date(10, 20)))

        // Le minuit qui tombe dans le délai efface encore.
        var soir = coupe
        soir.validerSeance(.pompes, date(9, 21), calendrier)
        soir.mettreAJour(date(10, 0))
        XCTAssertEqual(soir.soldeMinutes, 0)

        // Le minuit suivant est après le délai : le temps gagné reste.
        var lendemain = coupe
        lendemain.mettreAJour(date(10, 9))
        lendemain.validerSeance(.pompes, date(10, 10), calendrier)
        XCTAssertNil(lendemain.effacementLe)
        lendemain.mettreAJour(date(11, 9))
        XCTAssertEqual(lendemain.soldeMinutes, 15)
        XCTAssertNil(lendemain.hardcoreJusquA)
    }

    func testPendantLOnboardingCouperLeHardcoreEstImmediat() {
        var etat = hardcore(date(9, 9))
        etat.definirHardcore(false, date(9, 10), calendrier, immediat: true)
        XCTAssertFalse(etat.hardcoreEnVigueur(date(9, 10)))
        XCTAssertNil(etat.effacementLe)
    }

    func testReactiverLeHardcoreAnnuleLeDelai() {
        var etat = hardcore(date(9, 9))
        etat.definirHardcore(false, date(9, 20), calendrier, immediat: false)
        etat.definirHardcore(true, date(9, 21), calendrier, immediat: false)
        XCTAssertTrue(etat.hardcore)
        XCTAssertNil(etat.hardcoreJusquA)
    }

    // MARK: Séance libre

    func testRienTantQueLaSerieNEstPasFaite() {
        let regle = Regle(repetitions: 10, minutes: 15)
        XCTAssertEqual(regle.minutesPour(.pompes, 9), 0)
        XCTAssertEqual(regle.minutesPour(.pompes, 10), 15)
    }

    func testChaqueRepetitionEnPlusRapporteAuMemeRapport() {
        let regle = Regle(repetitions: 10, minutes: 15)
        XCTAssertEqual(regle.minutesPour(.pompes, 11), 16)
        XCTAssertEqual(regle.minutesPour(.pompes, 14), 21)
        XCTAssertEqual(regle.minutesPour(.pompes, 20), 30)
        // 15 squats valent 10 pompes.
        XCTAssertEqual(regle.minutesPour(.squats, 15), 15)
        XCTAssertEqual(regle.minutesPour(.squats, 30), 30)
    }

    func testLaSeanceLibreAjouteLeTempsDesRepetitionsFaites() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier, repetitions: 24)
        XCTAssertEqual(etat.soldeMinutes, 36)
        XCTAssertEqual(etat.seances.last?.repetitions, 24)
        XCTAssertEqual(etat.repetitionsDuJour(date(9, 11), calendrier), 24)
    }

    func testUneSeanceValideeVautAuMoinsLaSerie() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier, repetitions: 3)
        XCTAssertEqual(etat.soldeMinutes, 15)
        XCTAssertEqual(etat.seances.last?.repetitions, 10)
    }

    // MARK: Fenêtre suspendue (décompte à l'usage, fonctions pures)

    /// Une fenêtre de 15 minutes ouverte le lundi à 10 h.
    private func fenetreOuverteA10h() -> EtatJeu {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.ouvrirFenetre(date(9, 10), calendrier)
        return etat
    }

    func testQuitterLAppRendLeTempsNonUtilise() {
        var etat = fenetreOuverteA10h()
        XCTAssertEqual(etat.fenetreFin, date(9, 10, 15))
        // 4 min 30 passées dans l'app : il reste 10 min 30.
        etat.suspendreFenetre(date(9, 10, 4).addingTimeInterval(30))
        XCTAssertNil(etat.fenetreFin)
        XCTAssertEqual(etat.soldeMinutes, 10)
        XCTAssertEqual(etat.soldeSecondes, 30)
        XCTAssertEqual(etat.minutesAffichees, 11)
        XCTAssertFalse(etat.appsOuvertes(date(9, 10, 5), calendrier))
    }

    func testRouvrirLAppRepartAvecCeQuiReste() {
        var etat = fenetreOuverteA10h()
        etat.suspendreFenetre(date(9, 10, 4).addingTimeInterval(30))
        // Deux heures plus tard, le temps n'a pas bougé.
        etat.mettreAJour(date(9, 12))
        let fin = etat.ouvrirFenetre(date(9, 12), calendrier)
        XCTAssertEqual(fin, date(9, 12, 10).addingTimeInterval(30))
        XCTAssertEqual(etat.soldeEnSecondes, 0)
    }

    func testSuspendreNAjouteJamaisDeTemps() {
        var depart = EtatJeu()
        depart.validerSeance(.pompes, date(9, 10), calendrier)
        var etat = depart
        // Ouvrir et quitter dix fois de suite ne rapporte rien.
        for tour in 0..<10 {
            let instant = date(9, 10).addingTimeInterval(Double(tour) * 0.5)
            etat.ouvrirFenetre(instant, calendrier)
            etat.suspendreFenetre(instant.addingTimeInterval(0.4))
        }
        XCTAssertLessThanOrEqual(etat.soldeEnSecondes, depart.soldeEnSecondes)
    }

    func testSuspendreSansFenetreNeChangeRien() {
        var etat = EtatJeu()
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        var suspendu = etat
        suspendu.suspendreFenetre(date(9, 11))
        XCTAssertEqual(suspendu, etat)
        // Une fenêtre déjà finie ne rend rien.
        etat.ouvrirFenetre(date(9, 10), calendrier)
        etat.suspendreFenetre(date(9, 11))
        XCTAssertEqual(etat.soldeEnSecondes, 0)
        XCTAssertNil(etat.fenetreFin)
    }

    func testEnHardcoreLesSecondesAussiSEffacentAMinuit() {
        var etat = hardcore(date(9, 9))
        etat.validerSeance(.pompes, date(9, 10), calendrier)
        etat.ouvrirFenetre(date(9, 10), calendrier)
        etat.suspendreFenetre(date(9, 10, 4).addingTimeInterval(30))
        etat.mettreAJour(date(10, 0))
        XCTAssertEqual(etat.soldeEnSecondes, 0)
    }
}
