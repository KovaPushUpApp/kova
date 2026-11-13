import SwiftUI

/// Les écrans de l'onboarding, dans l'ordre du brief (section 7).
enum EtapeOnboarding: Int, CaseIterable, Hashable {
    case bienvenue, temps, choc, objectif, apps, regle, autoEcran, autoCamera, essai, paywall, offre

    var suivante: EtapeOnboarding? {
        EtapeOnboarding(rawValue: rawValue + 1)
    }

    var cleTitre: LocalizedStringKey {
        switch self {
        case .bienvenue: return "ecran.bienvenue"
        case .temps: return "ecran.temps"
        case .choc: return "ecran.choc"
        case .objectif: return "ecran.objectif"
        case .apps: return "ecran.apps"
        case .regle: return "ecran.regle"
        case .autoEcran: return "ecran.auto_ecran"
        case .autoCamera: return "ecran.auto_camera"
        case .essai: return "ecran.essai"
        case .paywall: return "ecran.paywall"
        case .offre: return "ecran.offre"
        }
    }
}

/// Les quatre onglets de la barre du bas.
enum Onglet: Int, CaseIterable, Hashable {
    case accueil, stats, planning, reglages

    var cleTitre: LocalizedStringKey {
        switch self {
        case .accueil: return "onglet.accueil"
        case .stats: return "onglet.stats"
        case .planning: return "onglet.planning"
        case .reglages: return "onglet.reglages"
        }
    }

    /// Pictogrammes provisoires. Les tracés des maquettes arrivent à l'étape 2.
    var symbole: String {
        switch self {
        case .accueil: return "house"
        case .stats: return "chart.bar"
        case .planning: return "calendar"
        case .reglages: return "gearshape"
        }
    }
}

/// Les écrans poussés par dessus les onglets (ils masquent la barre du bas).
enum Destination: Hashable {
    case exercice, placement, compteur, gagne, urgence, appsReglage, regle, abonnement, controleSocle

    var cleTitre: LocalizedStringKey {
        switch self {
        case .exercice: return "ecran.exercice"
        case .placement: return "ecran.placement"
        case .compteur: return "ecran.compteur"
        case .gagne: return "ecran.gagne"
        case .urgence: return "ecran.urgence"
        case .appsReglage: return "ecran.apps_reglage"
        case .regle: return "ecran.regle"
        case .abonnement: return "ecran.abonnement"
        case .controleSocle: return "socle.titre"
        }
    }

    /// Enchaînement de la séance : exercice, placement, compteur, gagné.
    var suivante: Destination? {
        switch self {
        case .exercice: return .placement
        case .placement: return .compteur
        case .compteur: return .gagne
        default: return nil
        }
    }
}
