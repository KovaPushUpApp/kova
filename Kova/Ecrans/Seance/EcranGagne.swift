import SwiftUI

/// Écran 17 : la récompense, en plein fond accent. "+15 min" qui défile, deux stats,
/// puis le choix : ouvrir maintenant ou garder son temps (brief, règle 3).
/// Les minutes sont déjà au solde quand cet écran s'affiche.
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

    /// Pendant une plage stricte, la fenêtre ne peut pas s'ouvrir (règle 5).
    private var plageEnCours: Bool {
        etat.jeu.plageActive(Date(), etat.calendrier)
    }

    private var texte: String {
        if plageEnCours {
            return L.t("gagne.texte_plage")
        }
        // On calcule sur une copie ce que donnerait l'ouverture maintenant,
        // plages strictes à venir comprises.
        let maintenant = Date()
        var essai = etat.jeu
        essai.ouvrirFenetre(maintenant, etat.calendrier)
        let fin = essai.finDOuverture(maintenant, etat.calendrier) ?? maintenant
        return L.f("gagne.texte", fin.formatted(date: .omitted, time: .shortened))
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
            Text(verbatim: texte)
                .paragraphe(Tokens.Couleurs.texteSurAccent)
                .padding(.top, Tokens.Espacements.e8)
                .entree(rang: 10)
            HStack(spacing: Tokens.Espacements.e13) {
                stat(String(etat.jeu.repetitionsDuJour(Date(), etat.calendrier)), L.t("gagne.repetitions_jour"))
                stat(L.n("commun.jours", etat.jeu.serie(Date(), etat.calendrier)), L.t("gagne.serie"))
            }
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 12)
        } bas: {
            if plageEnCours {
                BoutonPrincipal(L.t("gagne.garder"), style: .sombre, action: garder)
                    .entree(rang: 14)
            } else {
                BoutonPrincipal(L.t("gagne.ouvrir"), style: .sombre, action: ouvrir)
                    .entree(rang: 14)
                BoutonSecondaire(L.t("gagne.garder"), couleur: Tokens.Couleurs.texteSurAccent, action: garder)
            }
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
