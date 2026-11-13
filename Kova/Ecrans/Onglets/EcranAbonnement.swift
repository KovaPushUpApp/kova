import RevenueCat
import SwiftUI

/// Écran 23 : la formule actuelle, le renouvellement, la gestion dans l'App Store
/// et la restauration des achats.
struct EcranAbonnement: View {
    @EnvironmentObject private var abonnements: Abonnements
    @Environment(\.openURL) private var ouvrirLien
    let retour: () -> Void
    let ouvrirPaywall: () -> Void

    private var formule: Formule? { abonnements.formuleActuelle }

    private var nomFormule: String {
        switch formule {
        case .annuel: return L.t("formule.annuel")
        case .hebdomadaire: return L.t("formule.hebdomadaire")
        case .none: return L.t("formule.gratuite")
        }
    }

    private var prix: String {
        switch formule {
        case .annuel:
            let prix = abonnements.annuel?.storeProduct.localizedPriceString ?? L.t("prix.annuel")
            return L.f("abonnement.prix_annuel", prix)
        case .hebdomadaire:
            let prix = abonnements.hebdomadaire?.storeProduct.localizedPriceString ?? L.t("prix.hebdomadaire")
            return L.f("abonnement.prix_hebdo", prix)
        case .none:
            return L.t("abonnement.gratuit")
        }
    }

    private var renouvellement: String {
        guard let date = abonnements.abonnement?.expirationDate else { return "-" }
        return date.formatted(date: .abbreviated, time: .omitted)
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(spacing: Tokens.Espacements.e13) {
                BoutonRetour(action: retour)
                Text(verbatim: L.t("ecran.paywall")).titreCarte()
            }
            VStack(alignment: .leading, spacing: Tokens.Espacements.e5) {
                Text(verbatim: L.t("abonnement.actuelle"))
                    .legende(Tokens.Couleurs.texteSurAccent, graisse: .demiGras)
                Text(verbatim: nomFormule)
                    .font(.kovaTitre(Tokens.TaillesTexte.titreAccueil, relatifA: .largeTitle))
                    .foregroundColor(Tokens.Couleurs.texteSurAccent)
                Text(verbatim: prix)
                    .corps(Tokens.Couleurs.texteSurAccent)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .carte(fond: Tokens.Couleurs.accent)
            .padding(.top, Tokens.Espacements.e8)
            .pop(rang: 1)
            if formule != nil {
                GroupeReglages {
                    LigneReglage(L.t(abonnements.abonnement?.willRenew == false ? "abonnement.fin" : "abonnement.renouvellement")) {
                        Text(verbatim: renouvellement).legende()
                    }
                    LigneReglage(L.t("abonnement.essai")) {
                        Text(verbatim: L.t(abonnements.abonnement?.periodType == .trial ? "abonnement.essai.en_cours" : "abonnement.essai.termine"))
                            .legende()
                    }
                }
                .entree(rang: 3)
                Text(verbatim: L.t("abonnement.texte"))
                    .paragraphe(taille: Tokens.TaillesTexte.legende)
            }
        } bas: {
            if formule == nil {
                BoutonPrincipal(L.t("abonnement.voir"), action: ouvrirPaywall)
            } else {
                BoutonPrincipal(L.t("abonnement.gerer"), style: .bordure) {
                    if let lien = Liens.abonnements {
                        ouvrirLien(lien)
                    }
                }
            }
            BoutonSecondaire(L.t("commun.restaurer_achats")) {
                Task { _ = await abonnements.restaurer() }
            }
        }
        .alerteAchat(abonnements)
    }
}
