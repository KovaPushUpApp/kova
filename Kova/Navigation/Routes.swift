import Foundation

/// Les écrans de l'onboarding, dans l'ordre du brief (section 7).
enum EtapeOnboarding: Int, CaseIterable, Hashable {
    case bienvenue, temps, choc, objectif, apps, regle, autoEcran, autoCamera, essai, paywall, offre

    /// Étapes comptées par la barre d'avancement : de Temps à Essai.
    static let nombreEtapes = 8

    var suivante: EtapeOnboarding? {
        EtapeOnboarding(rawValue: rawValue + 1)
    }
}

/// Les quatre onglets de la barre du bas.
enum Onglet: Int, CaseIterable, Hashable {
    case accueil, stats, planning, reglages

    var titre: String {
        switch self {
        case .accueil: return L.t("onglet.accueil")
        case .stats: return L.t("onglet.stats")
        case .planning: return L.t("onglet.planning")
        case .reglages: return L.t("onglet.reglages")
        }
    }

    var icone: Icone.Nom {
        switch self {
        case .accueil: return .maison
        case .stats: return .stats
        case .planning: return .calendrier
        case .reglages: return .reglages
        }
    }
}

/// Les écrans poussés par dessus les onglets (ils masquent la barre du bas).
enum Destination: Hashable {
    case verrou, exercice, placement, compteur, gagne, urgence
    case appsReglage, regle, abonnement
    case controleSocle
}
