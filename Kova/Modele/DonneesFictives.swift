import Foundation

/// Données fictives de l'étape 2, reprises des maquettes.
/// Elles sont remplacées par les vraies données aux étapes 3 à 6.
enum DonneesFictives {
    static let soldeMinutes = 24
    static let serieJours = 6
    static let pompesAujourdHui = 40
    static let appsVerrouillees = ["Instagram", "TikTok", "YouTube"]

    /// Répétitions par jour, du lundi au dimanche. Vendredi est "aujourd'hui".
    static let semaine = [30, 50, 20, 60, 40, 0, 0]
    static let rangAujourdHui = 4
    static let seancesSemaine = 8
    static let progressionPourcent = 25
    static let heuresGagnees = 5

    static let urgencesRestantes = 1
    static let urgencesParSemaine = 2
    static let minutesUrgence = 5

    /// Rythme du comptage simulé, en secondes par répétition.
    static let rythmeEssai = 1.1
    static let rythmeSeance = 0.85
    static let repetitionsEssai = 3
    /// Délai avant "Corps entier détecté" sur l'écran de placement.
    static let delaiDetection = 1.2

    struct Plage: Identifiable {
        let id: Int
        let nom: String
        let horaires: String
        let jours: String
        var active: Bool
    }

    static var plages: [Plage] {
        [
            Plage(id: 0, nom: L.t("planning.fictif.travail"), horaires: L.f("planning.horaires", "9:00", "18:00"),
                  jours: L.t("planning.jours.semaine"), active: true),
            Plage(id: 1, nom: L.t("planning.fictif.nuit"), horaires: L.f("planning.horaires", "23:00", "7:00"),
                  jours: L.t("planning.jours.tous"), active: true),
            Plage(id: 2, nom: L.t("planning.fictif.revisions"), horaires: L.f("planning.horaires", "14:00", "17:00"),
                  jours: L.t("planning.jours.weekend"), active: false),
        ]
    }
}
