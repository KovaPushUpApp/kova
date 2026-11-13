import SwiftUI

@main
struct KovaApp: App {
    @StateObject private var etat = EtatApp()

    var body: some Scene {
        WindowGroup {
            VueRacine()
                .environmentObject(etat)
                .preferredColorScheme(.dark)
                .tint(Tokens.Couleurs.accent)
        }
    }
}
