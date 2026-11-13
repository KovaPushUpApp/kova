import UIKit

/// Retours haptiques : léger à chaque répétition, fort à la validation de la série.
enum Haptique {
    static func repetition() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }

    static func serieValidee() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
}
