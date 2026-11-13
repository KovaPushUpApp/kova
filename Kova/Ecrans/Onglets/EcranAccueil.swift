import SwiftUI

/// Écran 12 : l'accueil. Solde en très gros, série, bouton "Gagner du temps",
/// liste des apps verrouillées. Le temps d'usage par app de la maquette est
/// retiré (brief, section 9, contrainte 3).
struct EcranAccueil: View {
    @EnvironmentObject private var etat: EtatApp
    let gagnerDuTemps: () -> Void
    let ouvrirVerrou: () -> Void

    var body: some View {
        EcranKova {
            HStack {
                Text(verbatim: Tokens.nom)
                    .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                    .foregroundColor(Tokens.Couleurs.texte)
                Spacer()
                Text(verbatim: L.n("accueil.serie", DonneesFictives.serieJours))
                    .legende(Tokens.Couleurs.accent, graisse: .moyen)
            }
            solde
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            BoutonPrincipal(L.t("accueil.bouton"), pulse: true, action: gagnerDuTemps)
            VStack(alignment: .leading, spacing: Tokens.Espacements.e8) {
                Text(verbatim: L.t("accueil.apps"))
                    .tracking(Tokens.Typographie.interlettrageSurtitre * Tokens.TaillesTexte.legende)
                    .legende(graisse: .demiGras)
                    .textCase(.uppercase)
                    .accessibilityAddTraits(.isHeader)
                ForEach(Array(etat.apps.enumerated()), id: \.element) { rang, app in
                    Button(action: ouvrirVerrou) {
                        LigneApp(nom: app)
                    }
                    .buttonStyle(.plain)
                    .entree(rang: 4 + rang)
                }
            }
        }
    }

    private var solde: some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e5) {
            Text(verbatim: L.t("accueil.solde")).legende()
            HStack(alignment: .firstTextBaseline, spacing: Tokens.Espacements.e8) {
                Text(verbatim: String(DonneesFictives.soldeMinutes))
                    .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                    .foregroundColor(Tokens.Couleurs.texte)
                    .monospacedDigit()
                Text(verbatim: L.t("commun.min"))
                    .font(.kovaChiffre(Tokens.TaillesTexte.titreEcran))
                    .foregroundColor(Tokens.Couleurs.texteSecondaire)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: L.f("commun.minutes_court", DonneesFictives.soldeMinutes)))
            Text(verbatim: L.f("accueil.aujourdhui", DonneesFictives.pompesAujourdHui)).legende()
        }
    }
}
