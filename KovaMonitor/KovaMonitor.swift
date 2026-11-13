import DeviceActivity

/// Reverrouille à la fin d'une fenêtre et applique les plages du planning.
/// Squelette du socle : la logique arrive aux étapes 4 et 5.
/// Mémoire très limitée ici : pas de SDK, pas de SwiftUI, travail minimal.
final class KovaMonitor: DeviceActivityMonitor {
    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
    }

    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        super.intervalWillEndWarning(for: activity)
    }
}
