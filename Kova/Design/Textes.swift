import SwiftUI

/// Accès aux textes localisés (fichiers Localizable.strings, français et anglais).
enum L {
    /// Texte simple.
    static func t(_ cle: String) -> String {
        NSLocalizedString(cle, comment: "")
    }

    /// Texte avec valeurs. Dans les fichiers : %lld pour un entier, %@ pour un texte.
    static func f(_ cle: String, _ valeurs: CVarArg...) -> String {
        String(format: NSLocalizedString(cle, comment: ""), locale: Locale.current, arguments: valeurs)
    }

    /// Texte accordé en nombre : clés `cle.un` et `cle.autre`, avec le nombre en %lld.
    static func n(_ cle: String, _ nombre: Int) -> String {
        f(nombre == 1 ? cle + ".un" : cle + ".autre", nombre)
    }
}

extension Font {
    /// Très gros chiffres (68, 110, 178) : taille fixe, ils ne suivent pas la taille dynamique.
    static func kovaChiffre(_ taille: CGFloat) -> Font {
        .custom(Police.titres, fixedSize: taille)
    }
}

extension Text {
    /// Titre d'écran, 33.
    func titreEcran() -> some View {
        font(.kovaTitre(Tokens.TaillesTexte.titreEcran))
            .foregroundColor(Tokens.Couleurs.texte)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
    }

    /// Titre de sous-écran, 26.
    func titreCarte() -> some View {
        font(.kovaTitre(Tokens.TaillesTexte.titreCarte, relatifA: .title2))
            .foregroundColor(Tokens.Couleurs.texte)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
    }

    /// Paragraphe, 16, interligne du nombre d'or.
    func paragraphe(
        _ couleur: Color = Tokens.Couleurs.texteSecondaire,
        taille: CGFloat = Tokens.TaillesTexte.corps
    ) -> some View {
        font(.kovaTexte(taille))
            .lineSpacing(taille * (Tokens.Interligne.paragraphe - Tokens.Interligne.chiffre))
            .foregroundColor(couleur)
            .fixedSize(horizontal: false, vertical: true)
    }

    /// Légende, 13.
    func legende(
        _ couleur: Color = Tokens.Couleurs.texteSecondaire,
        graisse: GraisseTexte = .normal
    ) -> some View {
        font(.kovaTexte(Tokens.TaillesTexte.legende, graisse: graisse, relatifA: .caption))
            .foregroundColor(couleur)
    }

    /// Texte courant, 16.
    func corps(
        _ couleur: Color = Tokens.Couleurs.texte,
        graisse: GraisseTexte = .normal
    ) -> some View {
        font(.kovaTexte(graisse: graisse))
            .foregroundColor(couleur)
    }
}
