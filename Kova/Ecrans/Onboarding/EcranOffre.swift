import RevenueCat
import SwiftUI

/// Écran 11 : l'offre de rattrapage, en plein fond accent. Affichée une seule fois,
/// quand l'utilisateur ferme le paywall sans payer. La première année est à prix
/// réduit, puis l'abonnement se renouvelle au prix normal.
struct EcranOffre: View {
    @EnvironmentObject private var abonnements: Abonnements
    let fermer: () -> Void
    /// Appelé quand l'abonnement est actif après l'achat.
    let abonne: () -> Void

    private var prixReduit: String {
        abonnements.rattrapage?.storeProduct.introductoryDiscount?.localizedPriceString ?? L.t("prix.offre")
    }

    private var prixNormal: String {
        abonnements.rattrapage?.storeProduct.localizedPriceString ?? L.t("prix.annuel")
    }

    /// Remise en pourcentage, calculée sur les vrais prix quand ils sont connus.
    private var remise: Int {
        guard let produit = abonnements.rattrapage?.storeProduct,
              let reduit = produit.introductoryDiscount?.price,
              produit.price > 0 else {
            return 33
        }
        let part = NSDecimalNumber(decimal: (produit.price - reduit) / produit.price * 100)
        return Int(part.doubleValue.rounded())
    }

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
            Text(verbatim: L.f("offre.remise", remise))
                .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                .foregroundColor(Tokens.Couleurs.texteSurAccent)
                .lineLimit(1)
                .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
                .pop(rang: 2)
            Text(verbatim: L.f("offre.texte", prixReduit, prixNormal))
                .paragraphe(Tokens.Couleurs.texteSurAccent, taille: Tokens.TaillesTexte.corpsFort)
                .entree(rang: 6)
            HStack {
                VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
                    Text(verbatim: L.t("formule.annuel")).corps(graisse: .demiGras)
                    Text(verbatim: L.t("offre.detail")).legende()
                }
                Spacer(minLength: Tokens.Espacements.e8)
                VStack(alignment: .trailing, spacing: 0) {
                    Text(verbatim: prixReduit)
                        .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                        .foregroundColor(Tokens.Couleurs.texte)
                    Text(verbatim: prixNormal)
                        .strikethrough()
                        .legende()
                }
            }
            .carte(fond: Tokens.Couleurs.fond)
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 8)
            Text(verbatim: L.f("offre.mentions", prixReduit, prixNormal))
                .legende(Tokens.Couleurs.texteSurAccent)
                .fixedSize(horizontal: false, vertical: true)
            LiensLegaux(couleur: Tokens.Couleurs.texteSurAccent)
        } bas: {
            BoutonPrincipal(L.t("offre.bouton"), style: .sombre) {
                Task {
                    if await abonnements.acheter(abonnements.rattrapage) {
                        abonne()
                    }
                }
            }
            .opacity(abonnements.enCours ? Double(Tokens.Opacites.scan) : 1)
            .disabled(abonnements.enCours)
            .entree(rang: 10)
            BoutonSecondaire(L.t("offre.refuser"), couleur: Tokens.Couleurs.texteSurAccent, action: fermer)
        }
        .alerteAchat(abonnements)
        .onAppear { Analytique.envoyer(.offreVue) }
    }
}
