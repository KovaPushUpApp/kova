import Foundation

/// Stockage commun à l'app et à ses extensions (solde, règle, état du verrou).
enum GroupePartage {
    /// Identifiant de l'App Group, lu dans l'Info.plist de chaque cible
    /// (clé `KovaGroupePartage`, définie une seule fois dans project.yml).
    static var identifiant: String? {
        Bundle.main.object(forInfoDictionaryKey: "KovaGroupePartage") as? String
    }

    /// Réglages partagés. Dans le build du socle, l'App Group n'est pas encore
    /// déclaré : on retombe alors sur les réglages propres à l'app.
    static var reglages: UserDefaults {
        guard let identifiant, !identifiant.isEmpty,
              let partages = UserDefaults(suiteName: identifiant) else {
            return .standard
        }
        return partages
    }
}

/// Clés de stockage. Une seule liste pour l'app et les extensions.
enum CleStockage {
    static let onboardingTermine = "onboardingTermine"
}
