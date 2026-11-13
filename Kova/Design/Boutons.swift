import SwiftUI

/// Bouton principal : fond accent, hauteur 55.
struct BoutonPrincipal: View {
    let titre: LocalizedStringKey
    let action: () -> Void

    init(_ titre: LocalizedStringKey, action: @escaping () -> Void) {
        self.titre = titre
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(titre)
                .font(.kovaTitre(Tokens.TaillesTexte.corps, relatifA: .body))
                .foregroundColor(Tokens.Couleurs.texteSurAccent)
                .frame(maxWidth: .infinity)
                .frame(minHeight: Tokens.Hauteurs.boutonPrincipal)
                .background(Tokens.Couleurs.accent)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

/// Bouton secondaire : texte seul, hauteur 44.
struct BoutonSecondaire: View {
    let titre: LocalizedStringKey
    let action: () -> Void

    init(_ titre: LocalizedStringKey, action: @escaping () -> Void) {
        self.titre = titre
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(titre)
                .font(.kovaTexte(graisse: .moyen))
                .foregroundColor(Tokens.Couleurs.texteSecondaire)
                .frame(maxWidth: .infinity)
                .frame(minHeight: Tokens.Hauteurs.boutonSecondaire)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Petit bouton carré de retour, 44 x 44.
struct BoutonRetour: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "chevron.left")
                .font(.system(size: Tokens.TaillesTexte.corps, weight: .semibold))
                .foregroundColor(Tokens.Couleurs.texte)
                .frame(width: Tokens.Hauteurs.boutonIcone, height: Tokens.Hauteurs.boutonIcone)
                .background(Tokens.Couleurs.carte)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("commun.retour"))
    }
}
