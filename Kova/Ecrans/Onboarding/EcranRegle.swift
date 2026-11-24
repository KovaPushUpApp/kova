import SwiftUI

/// Écran 6 : la règle. Deux compteurs, répétitions et minutes, et le mode hardcore.
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
    @State private var hardcore = false

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
            carteHardcore
                .entree(rang: 4)
            if contexte == .reglages, !hardcore, let fin = etat.jeu.hardcoreJusquA {
                Text(verbatim: L.f("regle.hardcore.fin", fin.formatted(date: .abbreviated, time: .shortened)))
                    .legende(Tokens.Couleurs.accent)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            if contexte == .reglages, let le = etat.jeu.regleEnAttenteLe, etat.jeu.regleEnAttente != nil {
                Text(verbatim: L.f("regle.en_attente", le.formatted(date: .abbreviated, time: .shortened)))
                    .legende(Tokens.Couleurs.accent)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        } bas: {
            BoutonPrincipal(L.t("regle.bouton")) {
                // La nouvelle règle s'applique tout de suite, aussi depuis les réglages.
                etat.proposerRegle(brouillon, immediat: true)
                // Activer le mode hardcore est immédiat, le couper prend 24 h.
                if hardcore != etat.jeu.hardcore {
                    etat.definirHardcore(hardcore, immediat: contexte == .onboarding)
                }
                valider()
            }
            .entree(rang: 5)
        }
        .onAppear {
            brouillon = etat.jeu.regleEnAttente ?? etat.regle
            hardcore = etat.jeu.hardcore
        }
    }

    /// Mode hardcore : le temps gagné s'efface chaque jour à minuit.
    private var carteHardcore: some View {
        HStack(spacing: Tokens.Espacements.e13) {
            VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
                Text(verbatim: L.t("regle.hardcore"))
                    .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                    .foregroundColor(Tokens.Couleurs.texte)
                Text(verbatim: L.t(contexte == .onboarding ? "regle.hardcore.texte" : "regle.hardcore.texte_reglages"))
                    .legende()
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: Tokens.Espacements.e8)
            Interrupteur(actif: hardcore, libelle: L.t("regle.hardcore")) {
                hardcore.toggle()
            }
        }
        .carte()
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
