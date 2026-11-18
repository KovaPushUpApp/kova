import Foundation

/// Mode capture : ouvre l'app directement sur un écran, avec un état d'exemple.
///
/// Sert aux captures d'écran de la fiche App Store, prises dans le simulateur par
/// la CI (scripts/ci_captures.sh). L'app est lancée avec `-KovaCapture <écran>`.
/// Ce mode n'existe qu'en Debug : il est absent du build envoyé à Apple.
/// Il n'utilise ni la caméra, ni les achats, ni la mesure d'audience.
enum ModeCapture {
    enum Ecran: String {
        case accueil, verrou, gagne, stats, planning, paywall
    }

    /// L'écran demandé au lancement. Nil dans un lancement normal.
    static let ecran: Ecran? = {
        #if DEBUG
        return UserDefaults.standard.string(forKey: "KovaCapture").flatMap(Ecran.init(rawValue:))
        #else
        return nil
        #endif
    }()

    static var actif: Bool { ecran != nil }

    /// Onglet ouvert au lancement.
    static var onglet: Onglet {
        switch ecran {
        case .stats: return .stats
        case .planning: return .planning
        default: return .accueil
        }
    }

    /// Écrans poussés par dessus les onglets au lancement.
    static var chemin: [Destination] {
        switch ecran {
        case .verrou: return [.verrou]
        case .gagne: return [.gagne]
        default: return []
        }
    }

    /// Étapes de l'onboarding déjà empilées au lancement.
    static var etapes: [EtapeOnboarding] {
        ecran == .paywall ? [.paywall] : []
    }

    /// Les éléments verrouillés montrés à l'accueil : des catégories, sans nom de marque.
    static var apps: [String] {
        [L.t("capture.app.reseaux"), L.t("capture.app.videos"), L.t("capture.app.jeux")]
    }

    /// Écrit l'état d'exemple. À appeler avant la création de l'état de l'app.
    static func preparer(_ reglages: UserDefaults = GroupePartage.reglages) {
        guard let ecran else { return }
        let calendrier = Semaine.calendrier()
        let maintenant = Date()
        let aujourdHui = calendrier.startOfDay(for: maintenant)

        var jeu = EtatJeu()
        // Douze jours de suite, la séance du jour en dernier : c'est elle que
        // montre l'écran de récompense. Les jours de la semaine en cours sont plus
        // chargés que les précédents, pour que l'écran des stats montre une semaine
        // en hausse.
        let lundi = Semaine.debut(maintenant, calendrier)
        let cetteSemaine = [30, 25, 30, 20, 30, 25, 30]
        let joursDeSuite = 12
        for rang in 0..<joursDeSuite {
            let recul = joursDeSuite - 1 - rang
            guard let jour = calendrier.date(byAdding: .day, value: -recul, to: aujourdHui) else { continue }
            let faites = jour >= lundi ? cetteSemaine[Semaine.jour(jour, calendrier) - 1] : 10
            let date = recul == 0
                ? maintenant.addingTimeInterval(-60)
                : calendrier.date(bySettingHour: 18, minute: 30, second: 0, of: jour) ?? jour
            jeu.seances.append(Seance(
                date: date,
                exercice: .pompes,
                repetitions: faites,
                minutes: jeu.regle.minutesPour(.pompes, faites)
            ))
        }
        jeu.soldeMinutes = jeu.seances.last?.minutes ?? jeu.regle.minutes
        // Les plages ne sont écrites que pour l'écran du planning : ailleurs, une
        // plage en cours à l'heure de la capture changerait l'écran.
        if ecran == .planning {
            jeu.plages = [
                Plage(nom: L.t("capture.plage.travail"), debut: 9 * 60, fin: 12 * 60, jours: [1, 2, 3, 4, 5]),
                Plage(nom: L.t("capture.plage.nuit"), debut: 23 * 60, fin: 7 * 60, jours: Plage.tousLesJours),
            ]
        }
        DepotJeu.ecrire(jeu, reglages)
        reglages.set(ecran != .paywall, forKey: CleStockage.onboardingTermine)
        reglages.set(false, forKey: CleStockage.rappelQuotidien)
    }
}
