import Foundation
import UserNotifications

/// Notifications locales : le rappel quotidien, le rappel de fin d'essai, et
/// l'autorisation dont l'écran de verrou a besoin pour proposer d'ouvrir Kova
/// (brief, section 9, contrainte 4). Aucune notification ne vient d'un serveur.
enum Notifications {
    /// Ancien rappel, répété chaque jour avec un texte fixe. Retiré à chaque programmation.
    private static let ancienRappel = "rappel-quotidien"
    private static let prefixeRappel = "rappel-jour-"
    private static let finEssai = "fin-essai"
    /// Heure du rappel quotidien.
    private static let heureDuRappel = 18
    /// Nombre de jours programmés d'avance. Le texte d'une notification locale
    /// est figé quand on la programme : on prépare donc un rappel par jour, avec
    /// la série qui sera en jeu ce jour-là. Kova les reprogramme à chaque ouverture.
    private static let joursProgrammes = 14
    /// Délai le plus court accepté pour programmer un rappel, en secondes.
    private static let delaiMinimum: TimeInterval = 5
    /// Durée de l'essai gratuit de la formule annuelle, en jours.
    private static let joursEssai = 3

    /// Demande l'autorisation d'afficher des notifications.
    static func demanderAutorisation(_ suite: @escaping (Bool) -> Void = { _ in }) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { accorde, _ in
            DispatchQueue.main.async { suite(accorde) }
        }
    }

    private static var identifiantsDuRappel: [String] {
        [ancienRappel] + (0..<joursProgrammes).map { prefixeRappel + String($0) }
    }

    /// Titre et texte du rappel. Avec une série en cours, il dit ce qui est en jeu ce soir.
    static func texteDuRappel(serie: Int) -> (titre: String, texte: String) {
        if serie > 0 {
            return (L.n("rappel.serie.titre", serie), L.t("rappel.serie.texte"))
        }
        return (L.t("rappel.titre"), L.t("rappel.texte"))
    }

    /// Programme ou retire le rappel quotidien : une notification par jour à
    /// 18 h pour les jours à venir, sauf aujourd'hui si la journée est déjà gagnée.
    static func programmerRappel(actif: Bool, jeu: EtatJeu, calendrier: Calendar, maintenant: Date = Date()) {
        let centre = UNUserNotificationCenter.current()
        centre.removePendingNotificationRequests(withIdentifiers: identifiantsDuRappel)
        guard actif else { return }
        demanderAutorisation { accorde in
            guard accorde else { return }
            let aujourdHui = calendrier.startOfDay(for: maintenant)
            for decalage in 0..<joursProgrammes {
                guard let jour = calendrier.date(byAdding: .day, value: decalage, to: aujourdHui),
                      let instant = calendrier.date(bySettingHour: heureDuRappel, minute: 0, second: 0, of: jour) else {
                    continue
                }
                // Un délai nul ou négatif est refusé par le système.
                let delai = instant.timeIntervalSinceNow
                guard delai > delaiMinimum else { continue }
                if decalage == 0, jeu.repetitionsDuJour(maintenant, calendrier) > 0 {
                    continue
                }
                // La série telle qu'elle sera ce jour-là si aucune séance n'est faite d'ici là.
                let textes = texteDuRappel(serie: jeu.serie(instant, calendrier))
                let contenu = UNMutableNotificationContent()
                contenu.title = textes.titre
                contenu.body = textes.texte
                contenu.sound = .default
                let declencheur = UNTimeIntervalNotificationTrigger(timeInterval: delai, repeats: false)
                centre.add(UNNotificationRequest(
                    identifier: prefixeRappel + String(decalage),
                    content: contenu,
                    trigger: declencheur
                ))
            }
        }
    }

    /// À appeler quand l'essai gratuit démarre : le rappel part la veille du
    /// premier paiement. `prix` est le prix annuel affiché par l'App Store.
    static func programmerFinEssai(prix: String) {
        let centre = UNUserNotificationCenter.current()
        centre.removePendingNotificationRequests(withIdentifiers: [finEssai])
        demanderAutorisation { accorde in
            guard accorde else { return }
            let contenu = UNMutableNotificationContent()
            contenu.title = L.t("essai.rappel.titre")
            contenu.body = L.f("essai.rappel.texte", prix)
            contenu.sound = .default
            let delai = TimeInterval((joursEssai - 1) * 24 * 3600)
            let declencheur = UNTimeIntervalNotificationTrigger(timeInterval: delai, repeats: false)
            centre.add(UNNotificationRequest(identifier: finEssai, content: contenu, trigger: declencheur))
        }
    }
}
