import SwiftUI

/// Écran 3 : le compteur monte jusqu'au nombre de jours par an sur le téléphone.
struct EcranChoc: View {
    @EnvironmentObject private var etat: EtatApp
    @Environment(\.accessibilityReduceMotion) private var reduire
    let retour: () -> Void
    let suivant: () -> Void

    @State private var jours = 0
    private let minuterie = Timer.publish(
        every: Double(Tokens.Animations.CompteurChoc.intervalleMs) / 1000,
        on: .main,
        in: .common
    ).autoconnect()

    private var cible: Int { etat.tempsParJour.joursParAn }

    /// "plus de 2 ans" ou "10 mois", selon le temps déclaré.
    private var dureeSurDixAns: String {
        let mois = etat.tempsParJour.moisSurDixAns
        if mois >= 12 {
            return L.n("choc.plus_de_ans", mois / 12)
        }
        return L.n("choc.mois", mois)
    }

    var body: some View {
        EcranKova {
            EnTeteEtapes(etape: 2, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.f("choc.intro", etat.tempsParJour.heures))
                .corps(Tokens.Couleurs.texteSecondaire)
                .padding(.top, Tokens.Espacements.e34)
                .entree(rang: 1)
            compteur
            Text(verbatim: L.f("choc.texte", dureeSurDixAns))
                .paragraphe(Tokens.Couleurs.texte, taille: Tokens.TaillesTexte.corpsFort)
                .entree(rang: 13)
            carteAnnee
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 17)
        } bas: {
            BoutonPrincipal(L.t("choc.bouton"), action: suivant)
                .entree(rang: 21)
        }
        .onAppear {
            if reduire {
                jours = cible
            }
        }
        .onReceive(minuterie) { _ in
            if jours < cible {
                jours = min(cible, jours + Tokens.Animations.CompteurChoc.pas)
            }
        }
    }

    private var compteur: some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.Espacements.e8) {
            Text(verbatim: String(jours))
                .font(.kovaChiffre(Tokens.TaillesTexte.chiffreChoc))
                .foregroundColor(Tokens.Couleurs.accent)
                .monospacedDigit()
            Text(verbatim: L.t("choc.jours"))
                .font(.kovaTitre(Tokens.TaillesTexte.titreEcran))
                .foregroundColor(Tokens.Couleurs.texte)
        }
        .lineLimit(1)
        .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: L.f("a11y.choc", cible)))
    }

    private var carteAnnee: some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e13) {
            HStack {
                Text(verbatim: L.t("choc.annee")).legende(Tokens.Couleurs.texte)
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: L.f("choc.part", etat.tempsParJour.pourcentDeLAnnee)).legende()
            }
            GeometryReader { zone in
                ZStack(alignment: .leading) {
                    Tokens.Couleurs.ligne
                    Tokens.Couleurs.accent
                        .frame(width: zone.size.width * CGFloat(etat.tempsParJour.pourcentDeLAnnee) / 100)
                        .remplissageDepuisLaGauche(rang: 18)
                }
            }
            .frame(height: Tokens.Composants.BarreAnnee.hauteur)
            .clipShape(RoundedRectangle(cornerRadius: Tokens.Composants.BarreAnnee.arrondi, style: .continuous))
            .accessibilityHidden(true)
        }
        .carte()
    }
}
