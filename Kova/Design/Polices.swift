import SwiftUI
import UIKit

/// Noms PostScript des polices embarquées (dossier Resources/Fonts, déclarées dans UIAppFonts).
enum Police {
    static let titres = "SpaceGrotesk-Bold"
    static let texteNormal = "Inter-Regular"
    static let texteMoyen = "Inter-Medium"
    static let texteDemiGras = "Inter-SemiBold"

    static let toutes = [titres, texteNormal, texteMoyen, texteDemiGras]

    /// Vrai si la police est bien chargée par le système.
    static func estChargee(_ nom: String) -> Bool {
        UIFont(name: nom, size: Tokens.TaillesTexte.corps) != nil
    }
}

enum GraisseTexte {
    case normal, moyen, demiGras

    var police: String {
        switch self {
        case .normal: return Police.texteNormal
        case .moyen: return Police.texteMoyen
        case .demiGras: return Police.texteDemiGras
        }
    }
}

extension Font {
    /// Space Grotesk 700, pour les titres et les chiffres. Suit la taille dynamique.
    static func kovaTitre(_ taille: CGFloat, relatifA style: Font.TextStyle = .title) -> Font {
        .custom(Police.titres, size: taille, relativeTo: style)
    }

    /// Inter, pour tout le reste. Suit la taille dynamique.
    static func kovaTexte(
        _ taille: CGFloat = Tokens.TaillesTexte.corps,
        graisse: GraisseTexte = .normal,
        relatifA style: Font.TextStyle = .body
    ) -> Font {
        .custom(graisse.police, size: taille, relativeTo: style)
    }
}
