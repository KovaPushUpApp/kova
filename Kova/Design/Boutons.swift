import SwiftUI

/// Bouton principal, hauteur 55.
struct BoutonPrincipal: View {
    enum Style {
        /// Fond vert citron, texte sombre. Le cas courant.
        case accent
        /// Fond sombre, texte clair. Pour les écrans en plein fond accent.
        case sombre
        /// Contour vert citron, sans fond. Pour le déblocage d'urgence.
        case contour
        /// Contour gris fin, texte clair. Pour une action neutre.
        case bordure
    }

    let titre: String
    let style: Style
    let pulse: Bool
    let action: () -> Void

    init(_ titre: String, style: Style = .accent, pulse: Bool = false, action: @escaping () -> Void) {
        self.titre = titre
        self.style = style
        self.pulse = pulse
        self.action = action
    }

    private var couleurTexte: Color {
        switch style {
        case .accent: return Tokens.Couleurs.texteSurAccent
        case .sombre, .bordure: return Tokens.Couleurs.texte
        case .contour: return Tokens.Couleurs.accent
        }
    }

    private var couleurFond: Color {
        switch style {
        case .accent: return Tokens.Couleurs.accent
        case .sombre: return Tokens.Couleurs.fond
        case .contour, .bordure: return Color.clear
        }
    }

    private var couleurBordure: Color {
        switch style {
        case .accent, .sombre: return Color.clear
        case .contour: return Tokens.Couleurs.accent
        case .bordure: return Tokens.Couleurs.ligne
        }
    }

    private var police: Font {
        switch style {
        case .bordure: return .kovaTexte(graisse: .demiGras)
        case .accent, .sombre, .contour: return .kovaTitre(Tokens.TaillesTexte.corps, relatifA: .body)
        }
    }

    var body: some View {
        let forme = RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous)
        let bouton = Button(action: action) {
            Text(verbatim: titre)
                .font(police)
                .foregroundColor(couleurTexte)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .frame(minHeight: Tokens.Hauteurs.boutonPrincipal)
                .background(couleurFond)
                .clipShape(forme)
                .overlay(
                    forme.stroke(
                        couleurBordure,
                        lineWidth: style == .bordure ? Tokens.Traits.fin : Tokens.Traits.normal
                    )
                )
                .contentShape(forme)
        }
        .buttonStyle(.plain)

        if pulse {
            bouton.pulsation()
        } else {
            bouton
        }
    }
}

/// Bouton secondaire : texte seul, hauteur 44.
struct BoutonSecondaire: View {
    let titre: String
    let couleur: Color
    let action: () -> Void

    init(_ titre: String, couleur: Color = Tokens.Couleurs.texteSecondaire, action: @escaping () -> Void) {
        self.titre = titre
        self.couleur = couleur
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(verbatim: titre)
                .font(.kovaTexte(graisse: .moyen))
                .foregroundColor(couleur)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .frame(minHeight: Tokens.Hauteurs.boutonSecondaire)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Petit bouton carré avec pictogramme, 44 x 44.
struct BoutonIcone: View {
    let icone: Icone.Nom
    let libelle: String
    /// Vrai sur les écrans en plein fond accent : voile sombre, trait sombre.
    var surAccent = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Icone(
                nom: icone,
                couleur: surAccent ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texte
            )
            .frame(width: Tokens.Hauteurs.boutonIcone, height: Tokens.Hauteurs.boutonIcone)
            .background(
                surAccent
                    ? Tokens.Couleurs.texteSurAccent.opacity(Double(Tokens.Opacites.voileSurAccent))
                    : Tokens.Couleurs.carte
            )
            .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(verbatim: libelle))
    }
}

/// Bouton de retour des écrans poussés.
struct BoutonRetour: View {
    let action: () -> Void

    var body: some View {
        BoutonIcone(icone: .chevronGauche, libelle: L.t("commun.retour"), action: action)
    }
}

/// Bouton de fermeture (paywall, offre, séance).
struct BoutonFermer: View {
    var libelle: String = L.t("commun.fermer")
    var surAccent = false
    let action: () -> Void

    var body: some View {
        BoutonIcone(icone: .croix, libelle: libelle, surAccent: surAccent, action: action)
    }
}
