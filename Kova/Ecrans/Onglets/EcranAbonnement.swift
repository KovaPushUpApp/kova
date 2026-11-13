import SwiftUI

/// Écran 23 : la formule actuelle, le renouvellement, la gestion dans l'App Store
/// et la restauration. À l'étape 2, la formule et la date sont fictives.
struct EcranAbonnement: View {
    @Environment(\.openURL) private var ouvrirLien
    let retour: () -> Void
    let changerDeFormule: () -> Void
    let restaurer: () -> Void

    /// Page de gestion des abonnements d'Apple.
    private let gestionAppStore = URL(string: "https://apps.apple.com/account/subscriptions")

    /// Date fictive : dans 300 jours.
    private var renouvellement: String {
        Date().addingTimeInterval(300 * 24 * 3600).formatted(date: .abbreviated, time: .omitted)
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
                Text(verbatim: L.t("formule.annuel"))
                    .font(.kovaTitre(Tokens.TaillesTexte.titreAccueil, relatifA: .largeTitle))
                    .foregroundColor(Tokens.Couleurs.texteSurAccent)
                Text(verbatim: L.t("abonnement.prix_annuel"))
                    .corps(Tokens.Couleurs.texteSurAccent)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .carte(fond: Tokens.Couleurs.accent)
            .padding(.top, Tokens.Espacements.e8)
            .pop(rang: 1)
            GroupeReglages {
                LigneReglage(L.t("abonnement.renouvellement")) {
                    Text(verbatim: renouvellement).legende()
                }
                LigneReglage(L.t("abonnement.essai")) {
                    Text(verbatim: L.t("abonnement.essai.termine")).legende()
                }
                LigneReglageLien(
                    libelle: L.t("abonnement.hebdo"),
                    valeur: L.t("prix.hebdomadaire"),
                    action: changerDeFormule
                )
            }
            .entree(rang: 3)
            Text(verbatim: L.t("abonnement.texte"))
                .paragraphe(taille: Tokens.TaillesTexte.legende)
        } bas: {
            BoutonPrincipal(L.t("abonnement.gerer"), style: .bordure) {
                if let gestionAppStore {
                    ouvrirLien(gestionAppStore)
                }
            }
            BoutonSecondaire(L.t("commun.restaurer_achats"), action: restaurer)
        }
    }
}
