import Foundation
import ManagedSettings
import UserNotifications

/// Gère les deux boutons de l'écran de verrou.
///
/// Apple ne permet pas à cet écran d'ouvrir Kova directement (avant iOS 26.5).
/// "Faire mes pompes" envoie donc une notification à toucher, et note la
/// demande pour que Kova s'ouvre sur la séance. "Fermer" ferme l'app bloquée.
/// Le déblocage d'urgence se fait dans Kova (brief, section 9, contrainte 4).
final class KovaShieldAction: ShieldActionDelegate {
    private static let notification = "seance-demandee"

    override func handle(
        action: ShieldAction,
        for application: ApplicationToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        repondre(action, completionHandler)
    }

    override func handle(
        action: ShieldAction,
        for webDomain: WebDomainToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        repondre(action, completionHandler)
    }

    override func handle(
        action: ShieldAction,
        for category: ActivityCategoryToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        repondre(action, completionHandler)
    }

    private func repondre(_ action: ShieldAction, _ suite: @escaping (ShieldActionResponse) -> Void) {
        switch action {
        case .primaryButtonPressed:
            let reglages = GroupePartage.reglages
            reglages.set(true, forKey: CleStockage.seanceDemandee)
            reglages.set(Date().timeIntervalSince1970, forKey: CleStockage.seanceDemandeeLe)

            let contenu = UNMutableNotificationContent()
            contenu.title = NSLocalizedString("notification.titre", comment: "")
            contenu.body = NSLocalizedString("notification.texte", comment: "")
            contenu.sound = .default
            let demande = UNNotificationRequest(
                identifier: KovaShieldAction.notification,
                content: contenu,
                trigger: nil
            )
            UNUserNotificationCenter.current().add(demande) { _ in
                // L'écran de verrou se redessine, avec le rappel de toucher la notification.
                suite(.defer)
            }
        case .secondaryButtonPressed:
            suite(.close)
        @unknown default:
            suite(.close)
        }
    }
}
