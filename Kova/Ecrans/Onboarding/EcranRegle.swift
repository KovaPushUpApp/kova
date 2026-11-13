import SwiftUI

/// Écran 6 : la règle. Deux compteurs, répétitions et minutes.
/// Sert aussi depuis les réglages, sans la barre d'avancement.
struct EcranRegle: View {
    enum Contexte {
        case onboarding, reglages
    }

    @EnvironmentObject private var etat: EtatApp
    let contexte: Contexte
    let retour: () -> Void
    let valider: () -> Void

    @State private var brouillon: Regle = .parDefaut

    var body: some View {
        EcranKova {
            if contexte == .onboarding {
                EnTeteEtapes(etape: 6, total: EtapeOnboarding.nombreEtapes, retour: retour)
            } else {
                BoutonRetour(action: retour)
            }
            Text(verbatim: L.t("regle.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            Text(verbatim: L.t(contexte == .onboarding ? "regle.texte" : "regle.texte_reglages"))
                .paragraphe()
                .entree(rang: 2)
            compteurs
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 3)
            Text(verbatim: L.f("regle.par_heure", brouillon.pompesParHeure))
                .legende()
                .frame(maxWidth: .infinity)
            if contexte == .reglages, let le = etat.jeu.regleEnAttenteLe, etat.jeu.regleEnAttente != nil {
                Text(verbatim: L.f("regle.en_attente", le.formatted(date: .abbreviated, time: .shortened)))
                    .legende(Tokens.Couleurs.accent)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        } bas: {
            BoutonPrincipal(L.t("regle.bouton")) {
                // Dans les réglages, assouplir la règle prend effet 24 h plus tard.
                etat.proposerRegle(brouillon, immediat: contexte == .onboarding)
                valider()
            }
            .entree(rang: 5)
        }
        .onAppear { brouillon = etat.jeu.regleEnAttente ?? etat.regle }
    }

    private var compteurs: some View {
        VStack(spacing: Tokens.Espacements.e13) {
            compteur(
                valeur: brouillon.repetitions,
                unite: L.t("regle.unite.pompes"),
                moins: L.t("a11y.moins_de_pompes"),
                plus: L.t("a11y.plus_de_pompes"),
                changer: { pas in brouillon.changerRepetitions(de: pas) }
            )
            Text(verbatim: "=")
                .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                .foregroundColor(Tokens.Couleurs.texteSecondaire)
                .accessibilityHidden(true)
            compteur(
                valeur: brouillon.minutes,
                unite: L.t("regle.unite.minutes"),
                moins: L.t("a11y.moins_de_minutes"),
                plus: L.t("a11y.plus_de_minutes"),
                changer: { pas in brouillon.changerMinutes(de: pas) }
            )
        }
    }

    private func compteur(
        valeur: Int,
        unite: String,
        moins: String,
        plus: String,
        changer: @escaping (Int) -> Void
    ) -> some View {
        HStack {
            boutonPas("\u{2212}", libelle: moins) { changer(-Regle.pas) }
            Spacer(minLength: Tokens.Espacements.e8)
            VStack(spacing: 0) {
                Text(verbatim: String(valeur))
                    .font(.kovaChiffre(Tokens.TaillesTexte.chiffre))
                    .foregroundColor(Tokens.Couleurs.texte)
                    .monospacedDigit()
                Text(verbatim: unite).legende()
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: Tokens.Espacements.e8)
            boutonPas("+", libelle: plus) { changer(Regle.pas) }
        }
        .carte()
    }

    private func boutonPas(_ signe: String, libelle: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(verbatim: signe)
                .font(.kovaTitre(Tokens.TaillesTexte.titreCarte, relatifA: .title2))
                .foregroundColor(Tokens.Couleurs.texte)
                .frame(width: Tokens.Hauteurs.option, height: Tokens.Hauteurs.option)
                .background(Tokens.Couleurs.ligne)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(verbatim: libelle))
    }
}
