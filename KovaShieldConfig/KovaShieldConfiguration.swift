import Foundation
import ManagedSettings
import ManagedSettingsUI
import UIKit

/// Dessine l'écran de verrou aux couleurs de Kova, par dessus une app bloquée.
///
/// Apple limite cet écran à une icône, un titre, un sous-titre et deux boutons.
/// Il ne peut pas reproduire la maquette au pixel près (brief, section 9,
/// contrainte 4). Le nom de l'app bloquée n'est connu que dans cette extension.
final class KovaShieldConfiguration: ShieldConfigurationDataSource {
    /// Durée pendant laquelle l'écran rappelle de toucher la notification,
    /// après un appui sur "Faire mes pompes".
    private static let dureeDuRappel: TimeInterval = 20

    override func configuration(shielding application: Application) -> ShieldConfiguration {
        ecranDeVerrou(nom: application.localizedDisplayName)
    }

    override func configuration(
        shielding application: Application,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        ecranDeVerrou(nom: application.localizedDisplayName)
    }

    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        ecranDeVerrou(nom: webDomain.domain)
    }

    override func configuration(
        shielding webDomain: WebDomain,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        ecranDeVerrou(nom: webDomain.domain)
    }

    private func ecranDeVerrou(nom: String?) -> ShieldConfiguration {
        let titre: String
        if let nom, !nom.isEmpty {
            titre = String(format: texte("verrou.titre_app"), nom)
        } else {
            titre = texte("verrou.titre")
        }

        // Juste après un appui sur "Faire mes pompes", le sous-titre invite à
        // toucher la notification : l'écran de verrou ne peut pas ouvrir Kova.
        let demandeLe = GroupePartage.reglages.double(forKey: CleStockage.seanceDemandeeLe)
        let demandeRecente = Date().timeIntervalSince1970 - demandeLe < KovaShieldConfiguration.dureeDuRappel
        let sousTitre: String
        if demandeRecente {
            sousTitre = texte("verrou.notification")
        } else {
            let regle = DepotJeu.lire().regle
            sousTitre = String(format: texte("verrou.sous_titre"), regle.repetitions, regle.minutes)
        }

        return ShieldConfiguration(
            backgroundBlurStyle: .dark,
            backgroundColor: Tokens.CouleursUIKit.fond,
            icon: UIImage(named: "Logo"),
            title: ShieldConfiguration.Label(text: titre, color: Tokens.CouleursUIKit.texte),
            subtitle: ShieldConfiguration.Label(text: sousTitre, color: Tokens.CouleursUIKit.texteSecondaire),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: texte("verrou.bouton_principal"),
                color: Tokens.CouleursUIKit.texteSurAccent
            ),
            primaryButtonBackgroundColor: Tokens.CouleursUIKit.accent,
            secondaryButtonLabel: ShieldConfiguration.Label(
                text: texte("verrou.bouton_secondaire"),
                color: Tokens.CouleursUIKit.texteSecondaire
            )
        )
    }

    private func texte(_ cle: String) -> String {
        NSLocalizedString(cle, comment: "")
    }
}
