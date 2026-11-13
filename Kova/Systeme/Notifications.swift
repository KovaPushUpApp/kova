import Foundation
import UserNotifications

/// Notifications locales : le rappel quotidien, et l'autorisation dont l'écran
/// de verrou a besoin pour proposer d'ouvrir Kova (brief, section 9, contrainte 4).
/// Aucune notification ne vient d'un serveur.
enum Notifications {
    private static let rappel = "rappel-quotidien"
    /// Heure du rappel quotidien.
    private static let heureDuRappel = 18

    /// Demande l'autorisation d'afficher des notifications.
    static func demanderAutorisation(_ suite: @escaping (Bool) -> Void = { _ in }) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { accorde, _ in
            DispatchQueue.main.async { suite(accorde) }
        }
    }

    /// Programme ou retire le rappel quotidien.
    static func programmerRappel(actif: Bool) {
        let centre = UNUserNotificationCenter.current()
        centre.removePendingNotificationRequests(withIdentifiers: [rappel])
        guard actif else { return }
        demanderAutorisation { accorde in
            guard accorde else { return }
            let contenu = UNMutableNotificationContent()
            contenu.title = L.t("rappel.titre")
            contenu.body = L.t("rappel.texte")
            contenu.sound = .default
            var heure = DateComponents()
            heure.hour = heureDuRappel
            let declencheur = UNCalendarNotificationTrigger(dateMatching: heure, repeats: true)
            centre.add(UNNotificationRequest(identifier: rappel, content: contenu, trigger: declencheur))
        }
    }
}
