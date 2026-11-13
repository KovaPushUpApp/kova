import SwiftUI

/// L'app après l'onboarding : quatre onglets, et les écrans poussés par dessus.
struct VuePrincipale: View {
    @State private var onglet: Onglet = .accueil
    @State private var chemin: [Destination] = []
    @State private var paywallOuvert = false

    var body: some View {
        NavigationStack(path: $chemin) {
            VStack(spacing: 0) {
                contenuOnglet
                BarreOnglets(selection: $onglet)
                    .padding(.horizontal, Tokens.Ecran.margeCotes)
                    .padding(.bottom, Tokens.Espacements.e8)
            }
            .background(Tokens.Couleurs.fond.ignoresSafeArea())
            .navigationDestination(for: Destination.self) { destination in
                ecran(destination)
            }
        }
        .fullScreenCover(isPresented: $paywallOuvert) {
            // L'achat et la restauration arrivent à l'étape 6.
            EcranPaywall(
                fermer: { paywallOuvert = false },
                restaurer: {},
                acheter: { _ in paywallOuvert = false },
                ouvrirConditions: {},
                ouvrirConfidentialite: {}
            )
            .preferredColorScheme(.dark)
        }
    }

    @ViewBuilder
    private var contenuOnglet: some View {
        switch onglet {
        case .accueil:
            EcranAccueil(gagnerDuTemps: { aller(.exercice) }, ouvrirVerrou: { aller(.verrou) })
        case .stats:
            EcranStats()
        case .planning:
            // L'ajout d'une plage arrive à l'étape 5.
            EcranPlanning(ajouter: {})
        case .reglages:
            EcranReglages(ouvrir: aller)
        }
    }

    @ViewBuilder
    private func ecran(_ destination: Destination) -> some View {
        switch destination {
        case .verrou:
            EcranVerrou(
                commencer: { chemin = [.exercice] },
                fermer: reculer,
                urgence: { aller(.urgence) }
            )
        case .exercice:
            EcranExercice(retour: reculer, commencer: { aller(.placement) })
        case .placement:
            EcranPlacement(retour: reculer, suivant: { aller(.compteur) })
        case .compteur:
            EcranCompteur(abandonner: revenirAuxOnglets, valider: { aller(.gagne) })
        case .gagne:
            EcranGagne(ouvrir: revenirAuxOnglets, garder: revenirAuxOnglets)
        case .urgence:
            EcranUrgence(
                retour: reculer,
                debloquer: revenirAuxOnglets,
                faireSesPompes: { chemin = [.exercice] }
            )
        case .appsReglage:
            EcranAppsReglage(retour: reculer, enregistrer: reculer)
        case .regle:
            EcranRegle(contexte: .reglages, retour: reculer, valider: reculer)
        case .abonnement:
            EcranAbonnement(retour: reculer, changerDeFormule: { paywallOuvert = true }, restaurer: {})
        case .controleSocle:
            ControleSocle(retour: reculer)
        }
    }

    private func aller(_ destination: Destination) {
        chemin.append(destination)
    }

    private func reculer() {
        if !chemin.isEmpty {
            chemin.removeLast()
        }
    }

    private func revenirAuxOnglets() {
        chemin.removeAll()
    }
}

/// Barre d'onglets de la maquette : carte arrondie, onglet actif en fond accent.
struct BarreOnglets: View {
    @Binding var selection: Onglet

    var body: some View {
        HStack(spacing: Tokens.Espacements.e3) {
            ForEach(Onglet.allCases, id: \.self) { onglet in
                bouton(onglet)
            }
        }
        .padding(Tokens.Espacements.e8)
        .background(Tokens.Couleurs.carte)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
    }

    private func bouton(_ onglet: Onglet) -> some View {
        let actif = onglet == selection
        let couleur = actif ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texteSecondaire
        return Button {
            selection = onglet
        } label: {
            VStack(spacing: Tokens.Espacements.e3) {
                Icone(nom: onglet.icone, couleur: couleur)
                Text(verbatim: onglet.titre)
                    .legende(couleur, graisse: .demiGras)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .frame(height: Tokens.Hauteurs.onglet)
            .background(actif ? Tokens.Couleurs.accent : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(actif ? .isSelected : [])
    }
}
