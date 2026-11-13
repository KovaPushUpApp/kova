import Combine
import Foundation

/// État global de l'app. Tout reste sur le téléphone.
/// À l'étape 2, seules la fin de l'onboarding est enregistrée : le reste vit
/// en mémoire, et le stockage complet arrive à l'étape 5.
final class EtatApp: ObservableObject {
    private let reglages: UserDefaults

    @Published var onboardingTermine: Bool {
        didSet { reglages.set(onboardingTermine, forKey: CleStockage.onboardingTermine) }
    }

    // Réponses de l'onboarding.
    @Published var tempsParJour: TempsParJour = .cinqASix
    @Published var objectifs: Set<Objectif> = [.moinsScroller, .sport]
    @Published var regle: Regle = .parDefaut
    @Published var apps: [String] = DonneesFictives.appsVerrouillees

    // Séance en cours.
    @Published var exercice: Exercice = .pompes

    // L'offre de rattrapage ne s'affiche qu'une fois.
    @Published var offreDejaVue = false

    init(reglages: UserDefaults = GroupePartage.reglages) {
        self.reglages = reglages
        self.onboardingTermine = reglages.bool(forKey: CleStockage.onboardingTermine)
    }

    /// Répétitions demandées pour l'exercice choisi, selon la règle.
    var repetitionsDeLaSeance: Int {
        regle.repetitionsDemandees(pour: exercice)
    }
}
