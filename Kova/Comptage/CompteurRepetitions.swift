import CoreGraphics
import Foundation

/// Seuils du comptage. Ils viennent du brief (section 9) pour les angles, et
/// d'un premier réglage raisonnable pour le reste. À ajuster sur un vrai iPhone :
/// tant que le critère 3 du brief n'est pas mesuré, ces valeurs sont une hypothèse.
enum ReglagesComptage {
    /// Pompe : bas quand le coude passe sous 90°, haut quand il repasse au-dessus de 160°.
    static let coudeBas = 90.0
    static let coudeHaut = 160.0
    /// Squat : bas quand le genou passe sous 100°, haut quand il repasse au-dessus de 160°.
    static let genouBas = 100.0
    static let genouHaut = 160.0
    /// Le corps doit rester droit : angle épaule, hanche, cheville au minimum.
    static let alignementMinimum = 140.0
    /// Entre le haut et le bas, l'épaule (pompe) ou la hanche (squat) doit bouger
    /// d'au moins cette part de la longueur du membre.
    static let deplacementMinimum = 0.25
    /// Et l'appui (poignet ou cheville) doit rester presque immobile.
    static let appuiMaximum = 0.25
    /// Nombre d'images sur lesquelles l'angle est lissé.
    static let lissage = 5
    /// Images sans corps avant de mettre en pause.
    static let imagesAvantPause = 12
    /// Confiance minimale d'une articulation détectée.
    static let confianceMinimum: Float = 0.3
}

/// Compte les répétitions à partir des postures successives.
/// Une répétition ne compte que si le mouvement est complet : haut, bas, puis haut.
struct CompteurRepetitions {
    private enum Phase {
        case inconnue, haut, bas
    }

    let exercice: Exercice
    private(set) var compte = 0
    /// Faux quand le corps est sorti du cadre : la séance est en pause.
    private(set) var corpsVisible = false

    private var phase = Phase.inconnue
    private var angles: [Double] = []
    private var imagesSansCorps = 0
    /// Positions relevées en haut du mouvement, pour vérifier le déplacement.
    private var mobileEnHaut: CGPoint?
    private var appuiEnHaut: CGPoint?
    private var longueurEnHaut = 0.0
    /// Vrai si le passage en bas a respecté le déplacement attendu.
    private var basValide = false

    init(exercice: Exercice) {
        self.exercice = exercice
    }

    /// Traite la posture d'une image. Renvoie vrai si une répétition vient d'être comptée.
    mutating func traiter(_ pose: Pose?) -> Bool {
        guard let pose, let mesure = mesurer(pose) else {
            imagesSansCorps += 1
            if imagesSansCorps >= ReglagesComptage.imagesAvantPause {
                corpsVisible = false
                angles.removeAll()
            }
            return false
        }
        imagesSansCorps = 0
        corpsVisible = true

        guard mesure.postureCorrecte else {
            // Faux mouvement : on ignore l'image, sans casser la série en cours.
            return false
        }

        angles.append(mesure.angle)
        if angles.count > ReglagesComptage.lissage {
            angles.removeFirst(angles.count - ReglagesComptage.lissage)
        }
        let angle = angles.reduce(0, +) / Double(angles.count)

        let seuilBas = exercice == .squats ? ReglagesComptage.genouBas : ReglagesComptage.coudeBas
        let seuilHaut = exercice == .squats ? ReglagesComptage.genouHaut : ReglagesComptage.coudeHaut

        switch phase {
        case .inconnue, .haut:
            if angle > seuilHaut {
                phase = .haut
                mobileEnHaut = mesure.mobile
                appuiEnHaut = mesure.appui
                longueurEnHaut = mesure.longueur
            } else if angle < seuilBas, phase == .haut {
                phase = .bas
                basValide = deplacementCorrect(mesure)
            }
        case .bas:
            if !basValide {
                // Le déplacement peut être confirmé plus bas dans la descente.
                basValide = deplacementCorrect(mesure)
            }
            if angle > seuilHaut {
                phase = .haut
                let valide = basValide
                basValide = false
                mobileEnHaut = mesure.mobile
                appuiEnHaut = mesure.appui
                longueurEnHaut = mesure.longueur
                if valide {
                    compte += 1
                    return true
                }
            }
        }
        return false
    }

    // MARK: Mesure d'une image

    private struct Mesure {
        /// Angle suivi : coude pour la pompe, genou pour le squat.
        var angle: Double
        /// Point qui doit bouger : épaule (pompe) ou hanche (squat).
        var mobile: CGPoint
        /// Point d'appui qui doit rester en place : poignet ou cheville.
        var appui: CGPoint
        /// Longueur du membre, qui sert d'échelle.
        var longueur: Double
        var postureCorrecte: Bool
    }

    /// Choisit le côté du corps le mieux vu et en tire la mesure de l'image.
    private func mesurer(_ pose: Pose) -> Mesure? {
        for cote in [Cote.gauche, Cote.droit] {
            if let mesure = exercice == .squats ? mesurerSquat(pose, cote) : mesurerPompe(pose, cote) {
                return mesure
            }
        }
        return nil
    }

    private func mesurerPompe(_ pose: Pose, _ cote: Cote) -> Mesure? {
        guard let epaule = pose.point(cote.epaule),
              let coude = pose.point(cote.coude),
              let poignet = pose.point(cote.poignet) else {
            return nil
        }
        var correcte = true
        // Quand la hanche et la cheville (ou le genou) sont visibles, le corps doit être droit.
        if let hanche = pose.point(cote.hanche), let bas = pose.point(cote.cheville) ?? pose.point(cote.genou) {
            correcte = Geometrie.angle(epaule, hanche, bas) >= ReglagesComptage.alignementMinimum
        }
        return Mesure(
            angle: Geometrie.angle(epaule, coude, poignet),
            mobile: epaule,
            appui: poignet,
            longueur: Geometrie.distance(epaule, coude) + Geometrie.distance(coude, poignet),
            postureCorrecte: correcte
        )
    }

    private func mesurerSquat(_ pose: Pose, _ cote: Cote) -> Mesure? {
        guard let hanche = pose.point(cote.hanche),
              let genou = pose.point(cote.genou),
              let cheville = pose.point(cote.cheville) else {
            return nil
        }
        return Mesure(
            angle: Geometrie.angle(hanche, genou, cheville),
            mobile: hanche,
            appui: cheville,
            longueur: Geometrie.distance(hanche, genou) + Geometrie.distance(genou, cheville),
            postureCorrecte: true
        )
    }

    /// Vrai si, depuis le haut, le corps est vraiment descendu : le point mobile
    /// a bougé et l'appui est resté en place. Plier les bras debout, ou bouger
    /// seulement la tête, ne passe pas ce contrôle.
    private func deplacementCorrect(_ mesure: Mesure) -> Bool {
        guard let mobile = mobileEnHaut, let appui = appuiEnHaut, longueurEnHaut > 0 else {
            return false
        }
        let deplacement = Geometrie.distance(mobile, mesure.mobile) / longueurEnHaut
        let glissement = Geometrie.distance(appui, mesure.appui) / longueurEnHaut
        return deplacement >= ReglagesComptage.deplacementMinimum && glissement <= ReglagesComptage.appuiMaximum
    }
}
