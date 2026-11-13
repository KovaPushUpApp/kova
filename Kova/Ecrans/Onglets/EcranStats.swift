import SwiftUI

/// Écran 19 : la semaine. Total de répétitions, barres par jour, quatre cartes.
/// Les deux cartes "temps d'écran par jour" de la maquette sont remplacées par
/// des chiffres que Kova connaît : l'app ne peut pas lire le temps d'usage
/// (brief, section 9, contrainte 3).
struct EcranStats: View {
    @EnvironmentObject private var etat: EtatApp

    private var bilan: BilanSemaine {
        etat.jeu.bilanDeLaSemaine(Date(), etat.calendrier)
    }

    /// Initiales des jours, du lundi au dimanche, dans la langue du téléphone.
    private var initiales: [String] {
        let symboles = Calendar.current.veryShortStandaloneWeekdaySymbols
        guard symboles.count == 7 else { return Array(repeating: "", count: 7) }
        return Array(symboles[1...]) + [symboles[0]]
    }

    /// "9 – 15 nov." : la semaine en cours, du lundi au dimanche.
    private var periode: String {
        let lundi = Semaine.debut(Date(), etat.calendrier)
        guard let dimanche = etat.calendrier.date(byAdding: .day, value: 6, to: lundi) else { return "" }
        let format = DateIntervalFormatter()
        format.dateTemplate = "dMMM"
        return format.string(from: lundi, to: dimanche)
    }

    private var progression: String {
        guard let pourcent = bilan.progressionPourcent else { return L.t("stats.progression.aucune") }
        return L.f(pourcent >= 0 ? "stats.progression.hausse" : "stats.progression.baisse", abs(pourcent))
    }

    /// "1 h 15" ou "45 min".
    private var tempsGagne: String {
        let minutes = bilan.minutesGagnees
        if minutes >= 60 {
            return L.f("stats.gagne.heures", minutes / 60, minutes % 60)
        }
        return L.f("commun.minutes_court", minutes)
    }

    var body: some View {
        let semaine = bilan
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(alignment: .firstTextBaseline) {
                Text(verbatim: L.t("stats.titre")).titreEcran()
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: periode).legende()
            }
            graphique(semaine)
            HStack(spacing: Tokens.Espacements.e13) {
                carte(String(semaine.seances), L.t("stats.seances"), rang: 7)
                carte(progression, L.t("stats.progression"), rang: 8)
            }
            HStack(spacing: Tokens.Espacements.e13) {
                carte(L.n("commun.jours", etat.jeu.serie(Date(), etat.calendrier)), L.t("stats.serie"), rang: 9)
                carte(tempsGagne, L.t("stats.gagne"), rang: 10)
            }
        }
    }

    private func graphique(_ bilan: BilanSemaine) -> some View {
        let maximum = max(bilan.repetitionsParJour.max() ?? 1, 1)
        return VStack(spacing: Tokens.Espacements.e8) {
            HStack(alignment: .firstTextBaseline) {
                Text(verbatim: String(bilan.total))
                    .font(.kovaTitre(Tokens.TaillesTexte.titreAccueil, relatifA: .largeTitle))
                    .foregroundColor(Tokens.Couleurs.texte)
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: L.t("stats.total")).legende()
            }
            HStack(alignment: .bottom, spacing: Tokens.Espacements.e8) {
                ForEach(0..<7, id: \.self) { rang in
                    colonne(rang, bilan, maximum)
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
        .accessibilityLabel(Text(verbatim: L.f("a11y.stats", bilan.total)))
    }

    private func colonne(_ rang: Int, _ bilan: BilanSemaine, _ maximum: Int) -> some View {
        let valeur = rang < bilan.repetitionsParJour.count ? bilan.repetitionsParJour[rang] : 0
        let aujourdHui = rang == bilan.rangAujourdHui
        let hauteur = valeur > 0
            ? Tokens.Composants.Graphique.barreMax * CGFloat(valeur) / CGFloat(maximum)
            : Tokens.Composants.Graphique.barreVide
        let couleur: Color = aujourdHui && valeur > 0
            ? Tokens.Couleurs.accent
            : (valeur > 0 ? Tokens.Couleurs.barreInactive : Tokens.Couleurs.ligne)
        let initiale = rang < initiales.count ? initiales[rang] : ""
        return VStack(spacing: Tokens.Espacements.e8) {
            Spacer(minLength: 0)
            Text(verbatim: valeur > 0 ? String(valeur) : " ").legende()
            RoundedRectangle(cornerRadius: Tokens.Arrondis.petit, style: .continuous)
                .fill(couleur)
                .frame(height: hauteur)
                .croissanceDepuisLeBas(rang: 1 + rang)
            Text(verbatim: initiale)
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
