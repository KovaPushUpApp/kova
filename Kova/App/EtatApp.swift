import Foundation

/// État global de l'app. Tout reste sur le téléphone.
final class EtatApp: ObservableObject {
    private let reglages: UserDefaults

    @Published var onboardingTermine: Bool {
        didSet { reglages.set(onboardingTermine, forKey: CleStockage.onboardingTermine) }
    }

    init(reglages: UserDefaults = GroupePartage.reglages) {
        self.reglages = reglages
        self.onboardingTermine = reglages.bool(forKey: CleStockage.onboardingTermine)
    }
}
