import Foundation

/// Les exercices de Kova. Le taux de base est le nombre de répétitions
/// demandé quand la règle vaut 10 pompes (brief, section 6, règle 2).
enum Exercice: String, CaseIterable, Hashable {
    case pompes, squats, abdos, fentes, jumpingJacks

    /// Répétitions pour 10 pompes.
    var tauxDeBase: Int {
        switch self {
        case .pompes: return 10
        case .squats: return 15
        case .abdos: return 15
        case .fentes: return 12
        case .jumpingJacks: return 25
        }
    }

    /// Exercices dont le comptage est livré. Les autres arrivent quand il est fiable.
    static let disponibles: [Exercice] = [.pompes, .squats]

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

/// La règle du jeu : X pompes donnent Y minutes (brief, section 6, règle 1).
struct Regle: Equatable {
    static let pas = 5
    static let repetitionsPossibles = 5...100
    static let minutesPossibles = 5...120
    static let parDefaut = Regle(repetitions: 10, minutes: 15)

    private(set) var repetitions: Int
    private(set) var minutes: Int

    init(repetitions: Int, minutes: Int) {
        self.repetitions = Regle.borner(repetitions, Regle.repetitionsPossibles)
        self.minutes = Regle.borner(minutes, Regle.minutesPossibles)
    }

    private static func borner(_ valeur: Int, _ bornes: ClosedRange<Int>) -> Int {
        min(max(valeur, bornes.lowerBound), bornes.upperBound)
    }

    mutating func changerRepetitions(de pas: Int) {
        repetitions = Regle.borner(repetitions + pas, Regle.repetitionsPossibles)
    }

    mutating func changerMinutes(de pas: Int) {
        minutes = Regle.borner(minutes + pas, Regle.minutesPossibles)
    }

    /// Pompes à faire pour gagner une heure.
    var pompesParHeure: Int {
        Int((Double(repetitions) * 60 / Double(minutes)).rounded())
    }

    /// Répétitions demandées pour un exercice : même rapport que les pompes.
    func repetitionsDemandees(pour exercice: Exercice) -> Int {
        let base = Double(Exercice.pompes.tauxDeBase)
        return max(1, Int((Double(exercice.tauxDeBase) * Double(repetitions) / base).rounded()))
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
