import SwiftUI

/// Écran 12 : l'accueil. Solde en très gros, série, bouton "Gagner du temps",
/// liste des apps verrouillées. Quand une fenêtre est ouverte, le gros chiffre
/// devient le temps restant. Le temps d'usage par app de la maquette est retiré
/// (brief, section 9, contrainte 3).
struct EcranAccueil: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var apps: GestionApps
    let gagnerDuTemps: () -> Void
    let ouvrirVerrou: () -> Void

    var body: some View {
        EcranKova {
            HStack {
                Text(verbatim: Tokens.nom)
                    .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                    .foregroundColor(Tokens.Couleurs.texte)
                Spacer()
                Text(verbatim: L.n("accueil.serie", etat.jeu.serie(Date(), etat.calendrier)))
                    .legende(Tokens.Couleurs.accent, graisse: .moyen)
            }
            // Le bloc se recalcule chaque seconde pour suivre la fenêtre en cours.
            TimelineView(.periodic(from: Date(), by: 1)) { contexte in
                solde(contexte.date)
            }
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 1)
            BoutonPrincipal(L.t("accueil.bouton"), pulse: true, action: gagnerDuTemps)
            VStack(alignment: .leading, spacing: Tokens.Espacements.e8) {
                Text(verbatim: L.t("accueil.apps"))
                    .tracking(Tokens.Typographie.interlettrageSurtitre * Tokens.TaillesTexte.legende)
                    .legende(graisse: .demiGras)
                    .textCase(.uppercase)
                    .accessibilityAddTraits(.isHeader)
                Button(action: ouvrirVerrou) {
                    VueAppsChoisies(apps: apps)
                }
                .buttonStyle(.plain)
                .entree(rang: 4)
            }
        }
    }

    @ViewBuilder
    private func solde(_ maintenant: Date) -> some View {
        let fin = etat.jeu.finDOuverture(maintenant, etat.calendrier)
        let plage = etat.jeu.plageActive(maintenant, etat.calendrier)
        VStack(alignment: .leading, spacing: Tokens.Espacements.e5) {
            if let fin {
                Text(verbatim: L.f("accueil.ouvert_jusqua", fin.formatted(date: .omitted, time: .shortened)))
                    .legende(Tokens.Couleurs.accent)
                grosChiffre(minutesRestantes(fin, maintenant))
            } else {
                Text(verbatim: L.t("accueil.solde")).legende()
                grosChiffre(etat.jeu.soldeMinutes)
            }
            Text(verbatim: L.f("accueil.aujourdhui", etat.jeu.repetitionsDuJour(maintenant, etat.calendrier)))
                .legende()
            if plage && fin == nil {
                Text(verbatim: L.t("accueil.plage")).legende(Tokens.Couleurs.accent)
            } else if fin == nil && etat.jeu.soldeMinutes > 0 {
                BoutonSecondaire(L.t("accueil.ouvrir"), couleur: Tokens.Couleurs.accent) {
                    etat.ouvrirFenetre()
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Minutes restantes, arrondies vers le haut : il reste "1 min" jusqu'à la dernière seconde.
    private func minutesRestantes(_ fin: Date, _ maintenant: Date) -> Int {
        max(0, Int((fin.timeIntervalSince(maintenant) / 60).rounded(.up)))
    }

    private func grosChiffre(_ minutes: Int) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.Espacements.e8) {
            Text(verbatim: String(minutes))
                .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                .foregroundColor(Tokens.Couleurs.texte)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
            Text(verbatim: L.t("commun.min"))
                .font(.kovaChiffre(Tokens.TaillesTexte.titreEcran))
                .foregroundColor(Tokens.Couleurs.texteSecondaire)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: L.f("commun.minutes_court", minutes)))
    }
}
