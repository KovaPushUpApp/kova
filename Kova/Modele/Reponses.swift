import Foundation

/// Noms affichés des exercices (le modèle est dans Shared/Jeu).
extension Exercice {
    var nom: String {
        switch self {
        case .pompes: return L.t("exercice.pompes")
        case .squats: return L.t("exercice.squats")
        case .abdos: return L.t("exercice.abdos")
        case .fentes: return L.t("exercice.fentes")
        case .jumpingJacks: return L.t("exercice.jumping_jacks")
        }
    }
}

/// Réponse à "Combien de temps par jour sur ton téléphone ?".
enum TempsParJour: String, CaseIterable, Hashable {
    case moinsDeDeux, troisAQuatre, cinqASix, septOuPlus

    /// Heures retenues pour le calcul de l'écran Choc.
    var heures: Int {
        switch self {
        case .moinsDeDeux: return 2
        case .troisAQuatre: return 3
        case .cinqASix: return 5
        case .septOuPlus: return 7
        }
    }

    var libelle: String {
        switch self {
        case .moinsDeDeux: return L.t("temps.option.moins_de_2")
        case .troisAQuatre: return L.t("temps.option.3_a_4")
        case .cinqASix: return L.t("temps.option.5_a_6")
        case .septOuPlus: return L.t("temps.option.7_ou_plus")
        }
    }

    /// Jours par an passés sur le téléphone : heures par jour x 365 / 24.
    var joursParAn: Int {
        Int((Double(heures) * 365 / 24).rounded())
    }

    /// Part de l'année, en pourcentage.
    var pourcentDeLAnnee: Int {
        Int((Double(heures) * 100 / 24).rounded())
    }

    /// Mois passés sur le téléphone en 10 ans.
    var moisSurDixAns: Int {
        Int((Double(heures) * 120 / 24).rounded())
    }
}

/// Réponses à "Tu veux quoi, au fond ?".
enum Objectif: String, CaseIterable, Hashable {
    case moinsScroller, sport, concentration, sommeil

    var libelle: String {
        switch self {
        case .moinsScroller: return L.t("objectif.option.moins_scroller")
        case .sport: return L.t("objectif.option.sport")
        case .concentration: return L.t("objectif.option.concentration")
        case .sommeil: return L.t("objectif.option.sommeil")
        }
    }
}

/// Les deux formules du paywall.
enum Formule: String, CaseIterable, Hashable {
    case annuel, hebdomadaire
}
