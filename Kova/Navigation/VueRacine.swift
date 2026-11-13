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
        switch etape {
        case .bienvenue:
            // La restauration des achats arrive à l'étape 6.
            EcranBienvenue(commencer: { avancer(depuis: etape) }, restaurer: {})
        case .temps:
            EcranTemps(retour: reculer, suivant: { avancer(depuis: etape) })
        case .choc:
            EcranChoc(retour: reculer, suivant: { avancer(depuis: etape) })
        case .objectif:
            EcranObjectif(retour: reculer, suivant: { avancer(depuis: etape) })
        case .apps:
            EcranApps(retour: reculer, suivant: { avancer(depuis: etape) })
        case .regle:
            EcranRegle(contexte: .onboarding, retour: reculer, valider: { avancer(depuis: etape) })
        case .autoEcran:
            EcranAutorisation.tempsDEcran(retour: reculer, autoriser: { avancer(depuis: etape) })
        case .autoCamera:
            EcranAutorisation.camera(retour: reculer, autoriser: { avancer(depuis: etape) })
        case .essai:
            EcranEssai(retour: reculer, suivant: { avancer(depuis: etape) })
        case .paywall:
            EcranPaywall(
                fermer: fermerPaywall,
                restaurer: {},
                acheter: { _ in terminer() },
                ouvrirConditions: {},
                ouvrirConfidentialite: {}
            )
        case .offre:
            EcranOffre(fermer: terminer, prendre: terminer)
        }
    }

    private func avancer(depuis etape: EtapeOnboarding) {
        if let suivante = etape.suivante {
            chemin.append(suivante)
        }
    }

    private func reculer() {
        if !chemin.isEmpty {
            chemin.removeLast()
        }
    }

    /// Fermer le paywall sans payer montre l'offre de rattrapage, une seule fois.
    private func fermerPaywall() {
        if etat.offreDejaVue {
            terminer()
        } else {
            etat.offreDejaVue = true
            chemin.append(.offre)
        }
    }

    private func terminer() {
        etat.onboardingTermine = true
    }
}
