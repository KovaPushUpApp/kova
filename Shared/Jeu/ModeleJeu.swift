import Foundation

// Modèle des règles du jeu (brief, section 6). Foundation seulement : ce code
// est partagé avec les extensions, dont la mémoire est très limitée.

/// Les exercices de Kova. Le taux de base est le nombre de répétitions
/// demandé quand la règle vaut 10 pompes (règle 2).
enum Exercice: String, CaseIterable, Codable, Hashable {
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

    /// Exercices utilisables sans abonnement (brief, section 4).
    static let gratuits: [Exercice] = [.pompes]
}

/// La règle : X pompes donnent Y minutes (règle 1).
struct Regle: Codable, Equatable {
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

    /// Minutes gagnées par pompe. Plus c'est haut, plus la règle est souple.
    var minutesParPompe: Double {
        Double(minutes) / Double(repetitions)
    }

    /// Répétitions demandées pour un exercice : même rapport que les pompes.
    func repetitionsDemandees(pour exercice: Exercice) -> Int {
        let base = Double(Exercice.pompes.tauxDeBase)
        return max(1, Int((Double(exercice.tauxDeBase) * Double(repetitions) / base).rounded()))
    }

    /// Minutes gagnées pour une séance. Rien tant que la série de la règle n'est
    /// pas faite. Ensuite chaque répétition compte, au même rapport, arrondi à la
    /// minute en dessous.
    func minutesPour(_ exercice: Exercice, _ faites: Int) -> Int {
        let serie = repetitionsDemandees(pour: exercice)
        return faites < serie ? 0 : faites * minutes / serie
    }
}

/// Une plage stricte du planning (règle 5).
struct Plage: Encodable, Equatable, Identifiable {
    var id = UUID()
    var nom: String
    /// Début et fin, en minutes depuis minuit. Si la fin est avant le début,
    /// la plage passe minuit (23:00 à 7:00).
    var debut: Int
    var fin: Int
    /// Jours où la plage commence : 1 = lundi, 7 = dimanche.
    var jours: Set<Int>
    var active = true
    /// Une plage désactivée reste en vigueur jusqu'à cette date (règle 7).
    var enVigueurJusquA: Date?

    static let tousLesJours: Set<Int> = [1, 2, 3, 4, 5, 6, 7]
    /// Le système limite le nombre de surveillances (20 d'après les retours de
    /// développeurs). Chaque plage en prend 2, la fenêtre et l'urgence 1 chacune.
    static let nombreMaximum = 8
}

/// Une séance validée.
struct Seance: Codable, Equatable {
    var date: Date
    var exercice: Exercice
    var repetitions: Int
    var minutes: Int
}

/// Tout l'état du jeu, enregistré sur le téléphone dans l'App Group.
struct EtatJeu: Encodable, Equatable {
    static let minutesUrgence = 5
    static let urgencesParSemaine = 2
    /// Délai des assouplissements (règle 7), en secondes.
    static let delaiAssouplissement: TimeInterval = 24 * 3600
    /// Nombre de séances gardées en mémoire.
    static let historiqueMaximum = 600

    var regle = Regle.parDefaut
    /// Règle plus souple en attente, et la date à laquelle elle s'applique.
    var regleEnAttente: Regle?
    var regleEnAttenteLe: Date?

    var soldeMinutes = 0
    /// Les secondes en plus des minutes du solde (0 à 59) : le reste d'une
    /// fenêtre suspendue retourne au solde à la seconde près.
    var soldeSecondes = 0
    /// Mode hardcore : le temps gagné et la fenêtre ouverte s'effacent chaque jour à minuit.
    var hardcore = false
    /// Un mode hardcore coupé reste en vigueur jusqu'à cette date (règle 7).
    var hardcoreJusquA: Date?
    /// Le minuit où le temps gagné s'efface, quand le mode hardcore l'a programmé.
    var effacementLe: Date?
    /// Fin de la fenêtre de déblocage en cours.
    var fenetreFin: Date?
    /// Fin du déblocage d'urgence en cours.
    var urgenceFin: Date?
    /// Dates des déblocages d'urgence utilisés.
    var urgences: [Date] = []
    var urgenceActivee = true

    var plages: [Plage] = []
    var seances: [Seance] = []
    var exerciceParDefaut: Exercice = .pompes
}

// Lecture tolérante : une donnée absente prend sa valeur par défaut. Ainsi, une
// mise à jour de l'app qui ajoute un champ ne fait pas perdre le solde, les
// séances ni le planning déjà enregistrés.

extension Plage: Decodable {
    init(from decoder: Decoder) throws {
        let boite = try decoder.container(keyedBy: CodingKeys.self)
        self.init(
            id: try boite.decodeIfPresent(UUID.self, forKey: .id) ?? UUID(),
            nom: try boite.decodeIfPresent(String.self, forKey: .nom) ?? "",
            debut: try boite.decodeIfPresent(Int.self, forKey: .debut) ?? 0,
            fin: try boite.decodeIfPresent(Int.self, forKey: .fin) ?? 0,
            jours: try boite.decodeIfPresent(Set<Int>.self, forKey: .jours) ?? Plage.tousLesJours,
            active: try boite.decodeIfPresent(Bool.self, forKey: .active) ?? true,
            enVigueurJusquA: try boite.decodeIfPresent(Date.self, forKey: .enVigueurJusquA)
        )
    }
}

extension EtatJeu: Decodable {
    init(from decoder: Decoder) throws {
        let boite = try decoder.container(keyedBy: CodingKeys.self)
        self.init()
        regle = try boite.decodeIfPresent(Regle.self, forKey: .regle) ?? regle
        regleEnAttente = try boite.decodeIfPresent(Regle.self, forKey: .regleEnAttente)
        regleEnAttenteLe = try boite.decodeIfPresent(Date.self, forKey: .regleEnAttenteLe)
        soldeMinutes = try boite.decodeIfPresent(Int.self, forKey: .soldeMinutes) ?? soldeMinutes
        soldeSecondes = try boite.decodeIfPresent(Int.self, forKey: .soldeSecondes) ?? soldeSecondes
        hardcore = try boite.decodeIfPresent(Bool.self, forKey: .hardcore) ?? hardcore
        hardcoreJusquA = try boite.decodeIfPresent(Date.self, forKey: .hardcoreJusquA)
        effacementLe = try boite.decodeIfPresent(Date.self, forKey: .effacementLe)
        fenetreFin = try boite.decodeIfPresent(Date.self, forKey: .fenetreFin)
        urgenceFin = try boite.decodeIfPresent(Date.self, forKey: .urgenceFin)
        urgences = try boite.decodeIfPresent([Date].self, forKey: .urgences) ?? urgences
        urgenceActivee = try boite.decodeIfPresent(Bool.self, forKey: .urgenceActivee) ?? urgenceActivee
        plages = try boite.decodeIfPresent([Plage].self, forKey: .plages) ?? plages
        seances = try boite.decodeIfPresent([Seance].self, forKey: .seances) ?? seances
        exerciceParDefaut = try boite.decodeIfPresent(Exercice.self, forKey: .exerciceParDefaut) ?? exerciceParDefaut
    }
}
