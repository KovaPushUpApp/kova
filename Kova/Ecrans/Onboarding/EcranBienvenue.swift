import SwiftUI

/// Écran 1 : accroche.
struct EcranBienvenue: View {
    let commencer: () -> Void
    let restaurer: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e34) {
            LogoKova()
                .pop()
                .padding(.top, Tokens.Espacements.e144)
            Text(verbatim: L.t("bienvenue.titre"))
                .font(.kovaTitre(Tokens.TaillesTexte.titreAccueil, relatifA: .largeTitle))
                .foregroundColor(Tokens.Couleurs.texte)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .entree(rang: 4)
            Text(verbatim: L.t("bienvenue.texte"))
                .paragraphe()
                .entree(rang: 6)
        } bas: {
            BoutonPrincipal(L.t("bienvenue.commencer"), action: commencer)
                .entree(rang: 9)
            // Pas de compte en V1 : ce bouton remplace "J'ai déjà un compte" de la maquette.
            BoutonSecondaire(L.t("commun.restaurer_achats"), action: restaurer)
        }
    }
}
