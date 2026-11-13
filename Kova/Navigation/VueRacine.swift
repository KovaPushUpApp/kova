import SwiftUI

/// Aiguillage : onboarding tant qu'il n'est pas terminé, puis l'app.
struct VueRacine: View {
    @EnvironmentObject private var etat: EtatApp

    var body: some View {
        ZStack {
            Tokens.Couleurs.fond.ignoresSafeArea()
            if etat.onboardingTermine {
                VuePrincipale()
            } else {
                ParcoursOnboarding()
            }
        }
    }
}

/// Les 11 écrans de l'onboarding, enchaînés dans une pile de navigation.
struct ParcoursOnboarding: View {
    @EnvironmentObject private var etat: EtatApp
    @State private var chemin: [EtapeOnboarding] = []

    var body: some View {
        NavigationStack(path: $chemin) {
            ecran(.bienvenue)
                .navigationDestination(for: EtapeOnboarding.self) { etape in
                    ecran(etape)
                }
        }
    }

    @ViewBuilder
    private func ecran(_ etape: EtapeOnboarding) -> some View {
        EcranProvisoire(titre: etape.cleTitre, retour: retour(pour: etape)) {
            if let suivante = etape.suivante {
                BoutonPrincipal("commun.continuer") { chemin.append(suivante) }
            } else {
                BoutonPrincipal("socle.terminer_onboarding") { etat.onboardingTermine = true }
            }
        }
    }

    /// Pas de retour sur le premier écran.
    private func retour(pour etape: EtapeOnboarding) -> (() -> Void)? {
        if etape == .bienvenue {
            return nil
        }
        return { reculer() }
    }

    private func reculer() {
        if !chemin.isEmpty {
            chemin.removeLast()
        }
    }
}
