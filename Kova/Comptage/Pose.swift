import CoreGraphics
import Foundation

/// Les articulations utiles au comptage.
enum Articulation: Hashable, CaseIterable {
    case epauleGauche, coudeGauche, poignetGauche, hancheGauche, genouGauche, chevilleGauche
    case epauleDroite, coudeDroit, poignetDroit, hancheDroite, genouDroit, chevilleDroite
}

/// Un côté du corps, vu comme une chaîne d'articulations.
struct Cote {
    let epaule: Articulation
    let coude: Articulation
    let poignet: Articulation
    let hanche: Articulation
    let genou: Articulation
    let cheville: Articulation

    static let gauche = Cote(
        epaule: .epauleGauche, coude: .coudeGauche, poignet: .poignetGauche,
        hanche: .hancheGauche, genou: .genouGauche, cheville: .chevilleGauche
    )
    static let droit = Cote(
        epaule: .epauleDroite, coude: .coudeDroit, poignet: .poignetDroit,
        hanche: .hancheDroite, genou: .genouDroit, cheville: .chevilleDroite
    )
}

/// La posture détectée sur une image : la position des articulations fiables.
/// Les positions sont dans un repère où une unité vaut la hauteur de l'image,
/// pour que les angles ne soient pas déformés. Aucune image n'est gardée ici.
struct Pose {
    var points: [Articulation: CGPoint]

    func point(_ articulation: Articulation) -> CGPoint? {
        points[articulation]
    }

    /// Vrai si toutes ces articulations ont été vues.
    func voit(_ articulations: [Articulation]) -> Bool {
        articulations.allSatisfy { points[$0] != nil }
    }
}

enum Geometrie {
    static func distance(_ a: CGPoint, _ b: CGPoint) -> Double {
        Double(hypot(a.x - b.x, a.y - b.y))
    }

    /// Angle au sommet `b`, entre les segments b-a et b-c, en degrés (0 à 180).
    static func angle(_ a: CGPoint, _ b: CGPoint, _ c: CGPoint) -> Double {
        let v1 = CGPoint(x: a.x - b.x, y: a.y - b.y)
        let v2 = CGPoint(x: c.x - b.x, y: c.y - b.y)
        let norme = Double(hypot(v1.x, v1.y) * hypot(v2.x, v2.y))
        guard norme > 0 else { return 180 }
        let cosinus = Double(v1.x * v2.x + v1.y * v2.y) / norme
        return acos(min(1, max(-1, cosinus))) * 180 / .pi
    }
}
