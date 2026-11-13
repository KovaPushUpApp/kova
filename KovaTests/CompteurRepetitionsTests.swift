import CoreGraphics
import XCTest
@testable import Kova

/// Critère d'acceptation 3 du brief, sur des mouvements dessinés à la main :
/// 10 pompes correctes sont comptées 10, les faux mouvements sont comptés 0.
/// Ces tests valident la logique. Seul un essai sur un vrai iPhone valide
/// le comptage face à une vraie caméra.
final class CompteurRepetitionsTests: XCTestCase {
    private typealias Posture = [Articulation: CGPoint]

    // Pompe vue de côté : poignet fixe au sol, épaule qui descend.
    private let haut: Posture = [
        .epauleGauche: CGPoint(x: 0.5, y: 0.5), .coudeGauche: CGPoint(x: 0.5, y: 0.3),
        .poignetGauche: CGPoint(x: 0.5, y: 0.1), .hancheGauche: CGPoint(x: 0.9, y: 0.4),
        .chevilleGauche: CGPoint(x: 1.3, y: 0.3),
    ]
    private let bas: Posture = [
        .epauleGauche: CGPoint(x: 0.5, y: 0.22), .coudeGauche: CGPoint(x: 0.62, y: 0.16),
        .poignetGauche: CGPoint(x: 0.5, y: 0.1), .hancheGauche: CGPoint(x: 0.9, y: 0.21),
        .chevilleGauche: CGPoint(x: 1.3, y: 0.2),
    ]

    /// Posture intermédiaire : 0 = en haut, 1 = en bas.
    private func melange(_ a: Posture, _ b: Posture, _ part: CGFloat) -> Pose {
        var points: Posture = [:]
        for (articulation, depart) in a {
            let arrivee = b[articulation] ?? depart
            points[articulation] = CGPoint(
                x: depart.x + (arrivee.x - depart.x) * part,
                y: depart.y + (arrivee.y - depart.y) * part
            )
        }
        return Pose(points: points)
    }

    /// Une répétition : descente, pause, remontée, pause.
    private func repetition(_ a: Posture, _ b: Posture, amplitude: CGFloat = 1) -> [Pose?] {
        let pas = 12
        var images: [Pose?] = []
        for rang in 0...pas {
            images.append(melange(a, b, amplitude * CGFloat(rang) / CGFloat(pas)))
        }
        images += Array(repeating: melange(a, b, amplitude), count: 6)
        for rang in 0...pas {
            images.append(melange(a, b, amplitude * (1 - CGFloat(rang) / CGFloat(pas))))
        }
        images += Array(repeating: melange(a, b, 0), count: 6)
        return images
    }

    private func compter(_ exercice: Exercice, _ images: [Pose?]) -> CompteurRepetitions {
        var compteur = CompteurRepetitions(exercice: exercice)
        for image in images {
            _ = compteur.traiter(image)
        }
        return compteur
    }

    func testDixPompesCorrectesSontCompteesDix() {
        var images: [Pose?] = Array(repeating: melange(haut, bas, 0), count: 8)
        for _ in 0..<10 {
            images += repetition(haut, bas)
        }
        XCTAssertEqual(compter(.pompes, images).compte, 10)
    }

    func testBougerSeulementLaTeteNeCompteRien() {
        // Le corps ne bouge pas : aucune articulation suivie ne change.
        let images: [Pose?] = Array(repeating: melange(haut, bas, 0), count: 400)
        XCTAssertEqual(compter(.pompes, images).compte, 0)
    }

    func testPlierLesBrasDeboutNeCompteRien() {
        // Épaule immobile, poignet qui remonte : le coude plie, mais le corps ne descend pas.
        let brasTendu: Posture = [
            .epauleGauche: CGPoint(x: 0.5, y: 0.6), .coudeGauche: CGPoint(x: 0.5, y: 0.4),
            .poignetGauche: CGPoint(x: 0.5, y: 0.2),
        ]
        let brasPlie: Posture = [
            .epauleGauche: CGPoint(x: 0.5, y: 0.6), .coudeGauche: CGPoint(x: 0.5, y: 0.4),
            .poignetGauche: CGPoint(x: 0.62, y: 0.55),
        ]
        var images: [Pose?] = Array(repeating: melange(brasTendu, brasPlie, 0), count: 8)
        for _ in 0..<10 {
            images += repetition(brasTendu, brasPlie)
        }
        XCTAssertEqual(compter(.pompes, images).compte, 0)
    }

    func testDemiPompesNeComptentPas() {
        var images: [Pose?] = Array(repeating: melange(haut, bas, 0), count: 8)
        for _ in 0..<10 {
            images += repetition(haut, bas, amplitude: 0.4)
        }
        XCTAssertEqual(compter(.pompes, images).compte, 0)
    }

    func testCorpsHorsCadreMetEnPause() {
        var images: [Pose?] = Array(repeating: melange(haut, bas, 0), count: 8)
        images += repetition(haut, bas)
        var compteur = compter(.pompes, images)
        XCTAssertTrue(compteur.corpsVisible)
        for _ in 0..<ReglagesComptage.imagesAvantPause {
            _ = compteur.traiter(nil)
        }
        XCTAssertFalse(compteur.corpsVisible)
        XCTAssertEqual(compteur.compte, 1, "Sortir du cadre ne doit pas effacer les répétitions déjà comptées.")
    }

    func testDixSquatsSontComptesDix() {
        // Squat vu de côté : cheville fixe, hanche qui descend, genou qui plie.
        let debout: Posture = [
            .hancheGauche: CGPoint(x: 0.5, y: 0.6), .genouGauche: CGPoint(x: 0.5, y: 0.35),
            .chevilleGauche: CGPoint(x: 0.5, y: 0.1),
        ]
        let accroupi: Posture = [
            .hancheGauche: CGPoint(x: 0.36, y: 0.32), .genouGauche: CGPoint(x: 0.62, y: 0.32),
            .chevilleGauche: CGPoint(x: 0.5, y: 0.1),
        ]
        var images: [Pose?] = Array(repeating: melange(debout, accroupi, 0), count: 8)
        for _ in 0..<10 {
            images += repetition(debout, accroupi)
        }
        XCTAssertEqual(compter(.squats, images).compte, 10)
    }

    func testAngleDroit() {
        let angle = Geometrie.angle(CGPoint(x: 0, y: 1), CGPoint(x: 0, y: 0), CGPoint(x: 1, y: 0))
        XCTAssertEqual(angle, 90, accuracy: 0.001)
    }
}
