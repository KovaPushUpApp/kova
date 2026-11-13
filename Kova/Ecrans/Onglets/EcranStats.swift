import SwiftUI

/// Écran 19 : la semaine. Total de répétitions, barres par jour, quatre cartes.
/// Les deux cartes "temps d'écran par jour" de la maquette sont remplacées par
/// des chiffres que Kova connaît : l'app ne peut pas lire le temps d'usage
/// (brief, section 9, contrainte 3).
struct EcranStats: View {
    private let semaine = DonneesFictives.semaine

    private var total: Int { semaine.reduce(0, +) }
    private var maximum: Int { max(semaine.max() ?? 1, 1) }

    /// Initiales des jours, du lundi au dimanche, dans la langue du téléphone.
    private var initiales: [String] {
        let symboles = Calendar.current.veryShortStandaloneWeekdaySymbols
        guard symboles.count == 7 else { return Array(repeating: "", count: 7) }
        return Array(symboles[1...]) + [symboles[0]]
    }

    /// "9 – 15 nov." : la semaine en cours, du lundi au dimanche.
    private var periode: String {
        var calendrier = Calendar.current
        calendrier.firstWeekday = 2
        guard let intervalle = calendrier.dateInterval(of: .weekOfYear, for: Date()) else { return "" }
        let format = DateIntervalFormatter()
        format.dateTemplate = "dMMM"
        return format.string(from: intervalle.start, to: intervalle.end.addingTimeInterval(-1))
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(alignment: .firstTextBaseline) {
                Text(verbatim: L.t("stats.titre")).titreEcran()
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: periode).legende()
            }
            graphique
            HStack(spacing: Tokens.Espacements.e13) {
                carte(String(DonneesFictives.seancesSemaine), L.t("stats.seances"), rang: 7)
                carte(L.f("stats.progression.valeur", DonneesFictives.progressionPourcent), L.t("stats.progression"), rang: 8)
            }
            HStack(spacing: Tokens.Espacements.e13) {
                carte(L.n("commun.jours", DonneesFictives.serieJours), L.t("stats.serie"), rang: 9)
                carte(L.f("stats.gagne.valeur", DonneesFictives.heuresGagnees), L.t("stats.gagne"), rang: 10)
            }
        }
    }

    private var graphique: some View {
        VStack(spacing: Tokens.Espacements.e8) {
            HStack(alignment: .firstTextBaseline) {
                Text(verbatim: String(total))
                    .font(.kovaTitre(Tokens.TaillesTexte.titreAccueil, relatifA: .largeTitle))
                    .foregroundColor(Tokens.Couleurs.texte)
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: L.t("stats.total")).legende()
            }
            HStack(alignment: .bottom, spacing: Tokens.Espacements.e8) {
                ForEach(0..<semaine.count, id: \.self) { rang in
                    colonne(rang)
                }
            }
            .frame(height: Tokens.Composants.Graphique.hauteur)
        }
        .padding(.horizontal, Tokens.Espacements.e13)
        .padding(.top, Tokens.Espacements.e21)
        .padding(.bottom, Tokens.Espacements.e13)
        .background(Tokens.Couleurs.carte)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: L.f("a11y.stats", total)))
    }

    private func colonne(_ rang: Int) -> some View {
        let valeur = semaine[rang]
        let aujourdHui = rang == DonneesFictives.rangAujourdHui
        let hauteur = valeur > 0
            ? Tokens.Composants.Graphique.barreMax * CGFloat(valeur) / CGFloat(maximum)
            : Tokens.Composants.Graphique.barreVide
        let couleur: Color = aujourdHui
            ? Tokens.Couleurs.accent
            : (valeur > 0 ? Tokens.Couleurs.barreInactive : Tokens.Couleurs.ligne)
        return VStack(spacing: Tokens.Espacements.e8) {
            Spacer(minLength: 0)
            Text(verbatim: valeur > 0 ? String(valeur) : " ").legende()
            RoundedRectangle(cornerRadius: Tokens.Arrondis.petit, style: .continuous)
                .fill(couleur)
                .frame(height: hauteur)
                .croissanceDepuisLeBas(rang: 1 + rang)
            Text(verbatim: initiales[rang])
                .legende(aujourdHui ? Tokens.Couleurs.texte : Tokens.Couleurs.texteSecondaire, graisse: .demiGras)
        }
        .frame(maxWidth: .infinity)
    }

    private func carte(_ valeur: String, _ libelle: String, rang: Int) -> some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
            Text(verbatim: valeur).titreCarte()
            Text(verbatim: libelle).legende()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .carte(marge: Tokens.Espacements.e13)
        .accessibilityElement(children: .combine)
        .entree(rang: rang)
    }
}
