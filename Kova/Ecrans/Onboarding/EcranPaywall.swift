import SwiftUI

/// Écran 10 : le paywall. Bouton fermer visible tout de suite, "Restaurer" visible,
/// prix et renouvellement lisibles sous le bouton (brief, section 4).
/// À l'étape 2, les prix sont des textes fixes. RevenueCat arrive à l'étape 6.
struct EcranPaywall: View {
    let fermer: () -> Void
    let restaurer: () -> Void
    let acheter: (Formule) -> Void
    let ouvrirConditions: () -> Void
    let ouvrirConfidentialite: () -> Void

    @State private var formule: Formule = .annuel

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack {
                BoutonFermer(action: fermer)
                Spacer()
                Button(action: restaurer) {
                    Text(verbatim: L.t("paywall.restaurer"))
                        .legende(graisse: .moyen)
                        .padding(.horizontal, Tokens.Espacements.e8)
                        .frame(minHeight: Tokens.Hauteurs.boutonSecondaire)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            Text(verbatim: L.t("paywall.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e8)
                .entree(rang: 1)
            Text(verbatim: L.t("paywall.texte"))
                .paragraphe()
                .entree(rang: 2)
            VStack(alignment: .leading, spacing: Tokens.Espacements.e13) {
                LigneCoche(texte: L.t("paywall.argument.apps"))
                LigneCoche(texte: L.n("paywall.argument.exercices", Exercice.disponibles.count))
                LigneCoche(texte: L.t("paywall.argument.suivi"))
            }
            .entree(rang: 3)
            VStack(spacing: Tokens.Espacements.e8) {
                carteFormule(
                    .annuel,
                    nom: L.t("formule.annuel"),
                    detail: L.t("paywall.annuel.detail"),
                    prix: L.t("prix.annuel"),
                    etiquette: L.t("paywall.annuel.etiquette"),
                    couleurEtiquette: Tokens.Couleurs.accent
                )
                carteFormule(
                    .hebdomadaire,
                    nom: L.t("formule.hebdomadaire"),
                    detail: L.t("paywall.hebdo.detail"),
                    prix: L.t("prix.hebdomadaire"),
                    etiquette: L.t("paywall.hebdo.etiquette"),
                    couleurEtiquette: Tokens.Couleurs.texteSecondaire
                )
            }
            .padding(.top, Tokens.Espacements.e3)
            .entree(rang: 4)
        } bas: {
            BoutonPrincipal(
                L.t(formule == .annuel ? "paywall.bouton.essai" : "commun.continuer"),
                pulse: true
            ) {
                acheter(formule)
            }
            Text(verbatim: L.t(formule == .annuel ? "paywall.mentions.annuel" : "paywall.mentions.hebdo"))
                .legende()
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, Tokens.Espacements.e3)
            HStack(spacing: Tokens.Espacements.e21) {
                lienLegal(L.t("legal.conditions"), action: ouvrirConditions)
                lienLegal(L.t("legal.confidentialite"), action: ouvrirConfidentialite)
            }
        }
    }

    private func carteFormule(
        _ valeur: Formule,
        nom: String,
        detail: String,
        prix: String,
        etiquette: String,
        couleurEtiquette: Color
    ) -> some View {
        CarteOption(choisie: formule == valeur, action: { formule = valeur }) {
            HStack(spacing: Tokens.Espacements.e13) {
                VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
                    Text(verbatim: nom).corps(graisse: .demiGras)
                    Text(verbatim: detail).legende()
                }
                Spacer(minLength: Tokens.Espacements.e8)
                VStack(alignment: .trailing, spacing: Tokens.Espacements.e3) {
                    Text(verbatim: prix)
                        .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                        .foregroundColor(Tokens.Couleurs.texte)
                    Text(verbatim: etiquette).legende(couleurEtiquette, graisse: .demiGras)
                }
            }
            .padding(.vertical, Tokens.Espacements.e13)
        }
    }

    private func lienLegal(_ titre: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(verbatim: titre)
                .underline()
                .legende()
                .frame(minHeight: Tokens.Hauteurs.boutonSecondaire)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
