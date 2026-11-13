import SwiftUI

@main
struct KovaApp: App {
    @StateObject private var etat = EtatApp()
    @StateObject private var apps = GestionApps()
    @StateObject private var abonnements = Abonnements()

    var body: some Scene {
        WindowGroup {
            VueRacine()
                .environmentObject(etat)
                .environmentObject(apps)
                .environmentObject(abonnements)
                .preferredColorScheme(.dark)
                .tint(Tokens.Couleurs.accent)
                .onAppear {
                    Analytique.demarrer()
                    abonnements.demarrer()
                }
                .task {
                    // Après l'onboarding, on revérifie l'accès Temps d'écran à chaque
                    // lancement : le système ne redemande rien s'il est déjà accordé.
                    if etat.onboardingTermine {
                        _ = await apps.demanderAutorisation()
                        etat.rafraichir()
                    }
                }
        }
    }
}
