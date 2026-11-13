import Foundation

/// Lecture et écriture de l'état du jeu dans les réglages partagés de l'App Group.
/// L'app et les extensions passent toutes par ici.
enum DepotJeu {
    static func lire(_ reglages: UserDefaults = GroupePartage.reglages) -> EtatJeu {
        guard let donnees = reglages.data(forKey: CleStockage.etatJeu),
              let etat = try? JSONDecoder().decode(EtatJeu.self, from: donnees) else {
            return EtatJeu()
        }
        return etat
    }

    static func ecrire(_ etat: EtatJeu, _ reglages: UserDefaults = GroupePartage.reglages) {
        guard let donnees = try? JSONEncoder().encode(etat) else { return }
        reglages.set(donnees, forKey: CleStockage.etatJeu)
    }
}
