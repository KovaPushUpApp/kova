import SwiftUI

/// Écran 11 : l'offre de rattrapage, en plein fond accent. Affichée une seule fois,
/// quand l'utilisateur ferme le paywall sans payer.
struct EcranOffre: View {
    let fermer: () -> Void
    let prendre: () -> Void

    var body: some View {
        EcranKova(fond: Tokens.Couleurs.accent, espacement: Tokens.Espacements.e13) {
            HStack {
                Spacer()
                BoutonFermer(surAccent: true, action: fermer)
            }
            Text(verbatim: L.t("offre.surtitre"))
                .corps(Tokens.Couleurs.texteSurAccent, graisse: .demiGras)
                .padding(.top, Tokens.Espacements.e55)
                .entree(rang: 1)
            Text(verbatim: L.t("offre.remise"))
                .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                .foregroundColor(Tokens.Couleurs.texteSurAccent)
                .lineLimit(1)
                .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
                .pop(rang: 2)
            Text(verbatim: L.t("offre.texte"))
                .paragraphe(Tokens.Couleurs.texteSurAccent, taille: Tokens.TaillesTexte.corpsFort)
                .entree(rang: 6)
            HStack {
                VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
                    Text(verbatim: L.t("formule.annuel")).corps(graisse: .demiGras)
                    Text(verbatim: L.t("offre.detail")).legende()
                }
                Spacer(minLength: Tokens.Espacements.e8)
                VStack(alignment: .trailing, spacing: 0) {
                    Text(verbatim: L.t("prix.offre"))
                        .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                        .foregroundColor(Tokens.Couleurs.texte)
                    Text(verbatim: L.t("prix.annuel"))
                        .strikethrough()
                        .legende()
                }
            }
            .carte(fond: Tokens.Couleurs.fond)
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 8)
            Text(verbatim: L.t("offre.mentions"))
                .legende(Tokens.Couleurs.texteSurAccent)
                .fixedSize(horizontal: false, vertical: true)
        } bas: {
            BoutonPrincipal(L.t("offre.bouton"), style: .sombre, action: prendre)
                .entree(rang: 10)
            BoutonSecondaire(L.t("offre.refuser"), couleur: Tokens.Couleurs.texteSurAccent, action: fermer)
        }
    }
}
