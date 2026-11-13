import Foundation

/// Liens externes de l'app. Les adresses propres à Kova sont à renseigner dans
/// project.base.yml (KOVA_LIEN_CONFIDENTIALITE, KOVA_LIEN_CONDITIONS, KOVA_CONTACT).
enum Liens {
    private static func reglage(_ cle: String) -> URL? {
        guard let texte = Bundle.main.object(forInfoDictionaryKey: cle) as? String, !texte.isEmpty else {
            return nil
        }
        return URL(string: texte)
    }

    /// Conditions d'utilisation. Par défaut, le contrat de licence standard d'Apple.
    static var conditions: URL? {
        reglage("KovaLienConditions")
            ?? URL(string: "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
    }

    /// Politique de confidentialité. Obligatoire avant la mise en ligne.
    static var confidentialite: URL? {
        reglage("KovaLienConfidentialite")
    }

    /// Adresse de contact, ouverte dans l'app Mail.
    static var contact: URL? {
        guard let adresse = Bundle.main.object(forInfoDictionaryKey: "KovaContact") as? String,
              !adresse.isEmpty else {
            return nil
        }
        return URL(string: "mailto:" + adresse)
    }

    /// Page de gestion des abonnements d'Apple.
    static let abonnements = URL(string: "https://apps.apple.com/account/subscriptions")
}
