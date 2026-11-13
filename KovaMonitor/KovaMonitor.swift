import DeviceActivity
import Foundation

/// Reverrouille à la fin d'une fenêtre et applique les plages du planning,
/// même quand Kova est fermée.
///
/// Chaque rappel du système fait la même chose : relire l'état du jeu et poser
/// ou retirer les boucliers. Mémoire très limitée ici : pas de SDK, pas de SwiftUI.
final class KovaMonitor: DeviceActivityMonitor {
    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
        Verrouillage.appliquerDepuisLeDepot(marge: Verrouillage.margeDeRappel)
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        Verrouillage.appliquerDepuisLeDepot(marge: Verrouillage.margeDeRappel)
    }

    /// Aucun avertissement n'est programmé aujourd'hui. Si un jour il l'est,
    /// il fera comme les autres réveils.
    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        super.intervalWillEndWarning(for: activity)
        Verrouillage.appliquerDepuisLeDepot(marge: Verrouillage.margeDeRappel)
    }
}
