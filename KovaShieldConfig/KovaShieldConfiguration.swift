import ManagedSettings
import ManagedSettingsUI
import UIKit

/// Dessine l'écran de verrou aux couleurs de Kova.
/// Apple limite cet écran à un titre, un sous-titre, une icône et deux boutons.
final class KovaShieldConfiguration: ShieldConfigurationDataSource {
    override func configuration(shielding application: Application) -> ShieldConfiguration {
        ecranDeVerrou()
    }

    override func configuration(
        shielding application: Application,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        ecranDeVerrou()
    }

    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        ecranDeVerrou()
    }

    override func configuration(
        shielding webDomain: WebDomain,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        ecranDeVerrou()
    }

    private func ecranDeVerrou() -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .dark,
            backgroundColor: Tokens.CouleursUIKit.fond,
            icon: nil,
            title: ShieldConfiguration.Label(
                text: texte("verrou.titre"),
                color: Tokens.CouleursUIKit.texte
            ),
            subtitle: ShieldConfiguration.Label(
                text: texte("verrou.sous_titre"),
                color: Tokens.CouleursUIKit.texteSecondaire
            ),
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
