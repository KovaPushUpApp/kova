import SwiftUI

/// L'app après l'onboarding : quatre onglets, et les écrans poussés par dessus.
struct VuePrincipale: View {
    @EnvironmentObject private var etat: EtatApp
    @State private var onglet: Onglet = .accueil
    @State private var chemin: [Destination] = []

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
    }

    @ViewBuilder
    private var contenuOnglet: some View {
        EcranProvisoire(titre: onglet.cleTitre) {
            switch onglet {
            case .accueil:
                BoutonPrincipal("ecran.exercice") { chemin.append(.exercice) }
                BoutonSecondaire("ecran.urgence") { chemin.append(.urgence) }
            case .stats, .planning:
                EmptyView()
            case .reglages:
                BoutonSecondaire("ecran.apps_reglage") { chemin.append(.appsReglage) }
                BoutonSecondaire("ecran.regle") { chemin.append(.regle) }
                BoutonSecondaire("ecran.abonnement") { chemin.append(.abonnement) }
                BoutonSecondaire("socle.titre") { chemin.append(.controleSocle) }
                BoutonSecondaire("socle.revoir_onboarding") { etat.onboardingTermine = false }
            }
        }
    }

    @ViewBuilder
    private func ecran(_ destination: Destination) -> some View {
        if destination == .controleSocle {
            ControleSocle(retour: { reculer() })
        } else {
            EcranProvisoire(titre: destination.cleTitre, retour: { reculer() }) {
                if let suivante = destination.suivante {
                    BoutonPrincipal("commun.continuer") { chemin.append(suivante) }
                } else if destination == .gagne {
                    BoutonPrincipal("socle.retour_accueil") { chemin.removeAll() }
                }
            }
        }
    }

    private func reculer() {
        if !chemin.isEmpty {
            chemin.removeLast()
        }
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
        return Button {
            selection = onglet
        } label: {
            VStack(spacing: Tokens.Espacements.e3) {
                Image(systemName: onglet.symbole)
                    .font(.system(size: Tokens.TaillesTexte.corps, weight: .semibold))
                Text(onglet.cleTitre)
                    .font(.kovaTexte(Tokens.TaillesTexte.legende, graisse: .demiGras, relatifA: .caption))
                    .lineLimit(1)
            }
            .foregroundColor(actif ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texteSecondaire)
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
