import SwiftUI

/// Écran 17 : la récompense, en plein fond accent. "+15 min" qui défile, deux stats,
/// puis le choix : ouvrir maintenant ou garder son temps.
struct EcranGagne: View {
    @EnvironmentObject private var etat: EtatApp
    @Environment(\.accessibilityReduceMotion) private var reduire
    let ouvrir: () -> Void
    let garder: () -> Void

    @State private var minutes = 0
    private let minuterie = Timer.publish(
        every: Double(Tokens.Animations.CompteurGain.intervalleMs) / 1000,
        on: .main,
        in: .common
    ).autoconnect()

    private var cible: Int { etat.regle.minutes }

    /// Heure de fin de la fenêtre si elle démarre maintenant.
    private var heureDeFin: String {
        Date().addingTimeInterval(Double(cible) * 60).formatted(date: .omitted, time: .shortened)
    }

    var body: some View {
        EcranKova(fond: Tokens.Couleurs.accent, espacement: Tokens.Espacements.e13) {
            Text(verbatim: etat.exercice.texteValide(etat.repetitionsDeLaSeance))
                .corps(Tokens.Couleurs.texteSurAccent, graisse: .demiGras)
                .padding(.top, Tokens.Espacements.e89)
                .entree(rang: 1)
            Text(verbatim: L.f("gagne.minutes", minutes))
                .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                .foregroundColor(Tokens.Couleurs.texteSurAccent)
                .monospacedDigit()
                .fixedSize(horizontal: false, vertical: true)
                .pop(rang: 1)
                .accessibilityLabel(Text(verbatim: L.f("gagne.minutes", cible)))
            Text(verbatim: L.f("gagne.texte", heureDeFin))
                .paragraphe(Tokens.Couleurs.texteSurAccent)
                .padding(.top, Tokens.Espacements.e8)
                .entree(rang: 10)
            HStack(spacing: Tokens.Espacements.e13) {
                stat(
                    String(DonneesFictives.pompesAujourdHui + etat.repetitionsDeLaSeance),
                    L.f("gagne.aujourdhui", etat.exercice.nom.lowercased())
                )
                stat(L.n("commun.jours", DonneesFictives.serieJours), L.t("gagne.serie"))
            }
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 12)
        } bas: {
            BoutonPrincipal(L.t("gagne.ouvrir"), style: .sombre, action: ouvrir)
                .entree(rang: 14)
            BoutonSecondaire(L.t("gagne.garder"), couleur: Tokens.Couleurs.texteSurAccent, action: garder)
        }
        .onAppear {
            if reduire {
                minutes = cible
            }
        }
        .onReceive(minuterie) { _ in
            if minutes < cible {
                minutes = min(cible, minutes + Tokens.Animations.CompteurGain.pas)
            }
        }
    }

    private func stat(_ valeur: String, _ libelle: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
            Text(verbatim: valeur).titreCarte()
            Text(verbatim: libelle).legende()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .carte(fond: Tokens.Couleurs.fond)
        .accessibilityElement(children: .combine)
    }
}

extension Exercice {
    /// "10 pompes validées", accordé selon l'exercice.
    func texteValide(_ repetitions: Int) -> String {
        switch self {
        case .pompes: return L.f("gagne.valide.pompes", repetitions)
        case .squats: return L.f("gagne.valide.squats", repetitions)
        case .abdos: return L.f("gagne.valide.abdos", repetitions)
        case .fentes: return L.f("gagne.valide.fentes", repetitions)
        case .jumpingJacks: return L.f("gagne.valide.jumping_jacks", repetitions)
        }
    }
}
