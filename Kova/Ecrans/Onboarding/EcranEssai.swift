import SwiftUI

/// Message d'encouragement commun à l'essai et à la séance.
func messageDeSeance(faites: Int, total: Int) -> String {
    if faites >= total {
        return L.t("seance.validee")
    }
    if faites == 0 {
        return L.t("seance.en_position")
    }
    return L.f("seance.encore", total - faites)
}

/// Compteur géant "3/10" de l'essai et de la séance.
struct CompteurGeant: View {
    let faites: Int
    let total: Int
    let tailleTotal: CGFloat

    var body: some View {
        VStack(spacing: Tokens.Espacements.e3) {
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text(verbatim: String(faites))
                    .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                    .foregroundColor(Tokens.Couleurs.texte)
                    .monospacedDigit()
                Text(verbatim: "/" + String(total))
                    .font(.kovaChiffre(tailleTotal))
                    .foregroundColor(Tokens.Couleurs.texteSecondaire)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: L.f("a11y.progression_valeur", faites, total)))
            Text(verbatim: messageDeSeance(faites: faites, total: total))
                .corps(Tokens.Couleurs.texteSecondaire)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Écran 9 : le moment de preuve. Trois pompes comptées, puis le bouton apparaît.
/// À l'étape 2, le comptage est simulé. La caméra arrive à l'étape 3.
struct EcranEssai: View {
    let retour: () -> Void
    let suivant: () -> Void

    @State private var faites = 0
    private let total = DonneesFictives.repetitionsEssai
    private let minuterie = Timer.publish(every: DonneesFictives.rythmeEssai, on: .main, in: .common).autoconnect()

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            EnTeteEtapes(etape: 8, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.t("essai.titre"))
                .titreCarte()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            VueCameraFictive(hauteur: Tokens.Composants.VueCamera.hauteurEssai)
            CompteurGeant(faites: faites, total: total, tailleTotal: Tokens.TaillesTexte.titreEcran)
        } bas: {
            if faites >= total {
                BoutonPrincipal(L.t("essai.bouton"), action: suivant)
                    .entree()
            } else {
                Color.clear.frame(height: Tokens.Hauteurs.boutonPrincipal)
            }
        }
        .onReceive(minuterie) { _ in
            if faites < total {
                faites += 1
            }
        }
    }
}
