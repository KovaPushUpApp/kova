import RevenueCat
import SwiftUI

/// Alerte commune aux écrans d'achat : montre la dernière erreur des abonnements.
private struct AlerteAchat: ViewModifier {
    @ObservedObject var abonnements: Abonnements

    func body(content: Content) -> some View {
        content.alert(
            L.t("achat.titre"),
            isPresented: Binding(
                get: { abonnements.erreur != nil },
                set: { visible in
                    if !visible {
                        abonnements.erreur = nil
                    }
                }
            ),
            actions: {
                Button(L.t("commun.ok")) { abonnements.erreur = nil }
            },
            message: {
                Text(verbatim: abonnements.erreur ?? "")
            }
        )
    }
}

extension View {
    func alerteAchat(_ abonnements: Abonnements) -> some View {
        modifier(AlerteAchat(abonnements: abonnements))
    }
}

/// Écran 10 : le paywall. Règles du brief (section 4) : bouton fermer visible
/// tout de suite, "Restaurer" visible, prix et renouvellement lisibles sous le
/// bouton, liens vers les conditions et la confidentialité.
/// Les prix viennent de l'App Store. Les textes fixes ne servent que de secours
/// tant que les offres ne sont pas chargées.
struct EcranPaywall: View {
    @EnvironmentObject private var abonnements: Abonnements
    @Environment(\.openURL) private var ouvrirLien
    let fermer: () -> Void
    /// Appelé quand un abonnement est actif, après un achat ou une restauration.
    let abonne: () -> Void

    @State private var formule: Formule = .annuel

    private var prixAnnuel: String {
        abonnements.annuel?.storeProduct.localizedPriceString ?? L.t("prix.annuel")
    }

    private var prixHebdo: String {
        abonnements.hebdomadaire?.storeProduct.localizedPriceString ?? L.t("prix.hebdomadaire")
    }

    private var detailAnnuel: String {
        if let parSemaine = abonnements.annuel?.storeProduct.localizedPricePerWeek {
            return L.f("paywall.annuel.par_semaine", parSemaine)
        }
        return L.t("paywall.annuel.detail")
    }

    private var avecEssai: Bool {
        formule == .annuel && abonnements.essaiDisponible
    }

    private var mentions: String {
        if formule == .hebdomadaire {
            return L.f("paywall.mentions.hebdo", prixHebdo)
        }
        return L.f(avecEssai ? "paywall.mentions.annuel_essai" : "paywall.mentions.annuel", prixAnnuel)
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack {
                BoutonFermer {
                    Analytique.envoyer(.paywallFerme)
                    fermer()
                }
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
                    detail: detailAnnuel,
                    prix: prixAnnuel,
                    etiquette: L.t(abonnements.essaiDisponible ? "paywall.annuel.etiquette" : "paywall.annuel.par_an"),
                    couleurEtiquette: Tokens.Couleurs.accent
                )
                carteFormule(
                    .hebdomadaire,
                    nom: L.t("formule.hebdomadaire"),
                    detail: L.t("paywall.hebdo.detail"),
                    prix: prixHebdo,
                    etiquette: L.t("paywall.hebdo.etiquette"),
                    couleurEtiquette: Tokens.Couleurs.texteSecondaire
                )
            }
            .padding(.top, Tokens.Espacements.e3)
            .entree(rang: 4)
        } bas: {
            BoutonPrincipal(L.t(avecEssai ? "paywall.bouton.essai" : "commun.continuer"), pulse: true, action: acheter)
                .opacity(abonnements.enCours ? Double(Tokens.Opacites.scan) : 1)
                .disabled(abonnements.enCours)
            Text(verbatim: mentions)
                .legende()
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, Tokens.Espacements.e3)
            LiensLegaux()
        }
        .alerteAchat(abonnements)
        .onAppear { Analytique.envoyer(.paywallVu) }
    }

    private func acheter() {
        let forfait = formule == .annuel ? abonnements.annuel : abonnements.hebdomadaire
        Task {
            if await abonnements.acheter(forfait) {
                abonne()
            }
        }
    }

    private func restaurer() {
        Task {
            if await abonnements.restaurer() {
                abonne()
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
}

/// Liens vers les conditions d'utilisation et la politique de confidentialité.
struct LiensLegaux: View {
    @Environment(\.openURL) private var ouvrirLien
    var couleur: Color = Tokens.Couleurs.texteSecondaire

    var body: some View {
        HStack(spacing: Tokens.Espacements.e21) {
            lien(L.t("legal.conditions"), Liens.conditions)
            lien(L.t("legal.confidentialite"), Liens.confidentialite)
        }
    }

    @ViewBuilder
    private func lien(_ titre: String, _ adresse: URL?) -> some View {
        if let adresse {
            Button {
                ouvrirLien(adresse)
            } label: {
                Text(verbatim: titre)
                    .underline()
                    .legende(couleur)
                    .frame(minHeight: Tokens.Hauteurs.boutonSecondaire)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }
}
