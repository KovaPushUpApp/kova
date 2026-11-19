import AVFoundation
import Combine
import Foundation
import Vision

/// Le squelette vu par la caméra, pour le dessiner par dessus l'aperçu. Il est à
/// part pour que seul le dessin se refasse à chaque image, pas tout l'écran.
/// Ce ne sont que des positions d'articulations : aucune image n'est gardée.
final class SqueletteCamera: ObservableObject {
    /// Positions dans un repère où une unité vaut la hauteur de l'image,
    /// verticale vers le haut.
    @Published fileprivate(set) var points: [Articulation: CGPoint] = [:]
    /// Largeur de l'image divisée par sa hauteur.
    fileprivate(set) var rapport: CGFloat = 0
}

/// La caméra d'une séance : elle filme, détecte la posture sur le téléphone et
/// compte les répétitions.
///
/// Confidentialité (brief, règle 10) : chaque image est analysée en mémoire puis
/// oubliée. Cette classe n'a aucune sortie photo ou vidéo, n'écrit rien sur le
/// disque et n'envoie rien sur le réseau. Seuls un nombre de répétitions et un
/// "corps visible ou non" en sortent.
final class CameraSeance: NSObject, ObservableObject {
    enum Etat {
        /// Caméra éteinte.
        case arretee
        /// Caméra allumée, comptage en cours.
        case active
        /// L'utilisateur a refusé l'accès à la caméra.
        case refusee
        /// Pas de caméra utilisable sur cet appareil.
        case indisponible
    }

    @Published private(set) var etat: Etat = .arretee
    @Published private(set) var repetitions = 0
    @Published private(set) var corpsVisible = false

    /// Le squelette détecté sur la dernière image.
    let squelette = SqueletteCamera()

    /// Session de capture, pour l'aperçu à l'écran.
    let session = AVCaptureSession()

    /// Appelé sur le fil principal à chaque répétition comptée, avec le total.
    var surRepetition: ((Int) -> Void)?

    // Tout ce qui suit n'est touché que depuis la file de la caméra.
    private let file = DispatchQueue(label: "kova.camera", qos: .userInitiated)
    private let sortie = AVCaptureVideoDataOutput()
    private let requete = VNDetectHumanBodyPoseRequest()
    private var configuree = false
    private var compteur = CompteurRepetitions(exercice: .pompes)
    /// Numéro du comptage en cours sur la file de la caméra.
    private var serieDeLaFile = 0

    // Ce qui suit n'est touché que depuis le fil principal.
    private var objectif = 0
    /// Numéro du comptage en cours. Il change à chaque démarrage et à chaque
    /// arrêt, pour écarter les images d'un comptage précédent arrivées en retard.
    private var serie = 0

    private var minuterieSimulee: Timer?

    /// Vrai si la caméra peut filmer sur cet appareil (faux dans le simulateur).
    static var disponible: Bool {
        #if targetEnvironment(simulator)
        return false
        #else
        return true
        #endif
    }

    /// Allume la caméra et remet le compteur à zéro. La caméra s'éteint seule
    /// dès que l'objectif est atteint.
    func demarrer(exercice: Exercice, objectif: Int) {
        serie += 1
        self.objectif = objectif
        repetitions = 0
        corpsVisible = false
        squelette.points = [:]
        guard CameraSeance.disponible else {
            simuler(objectif: objectif)
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            lancer(exercice: exercice, objectif: objectif)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] accorde in
                DispatchQueue.main.async {
                    if accorde {
                        self?.lancer(exercice: exercice, objectif: objectif)
                    } else {
                        self?.etat = .refusee
                    }
                }
            }
        default:
            etat = .refusee
        }
    }

    /// Éteint la caméra.
    func arreter() {
        serie += 1
        minuterieSimulee?.invalidate()
        minuterieSimulee = nil
        etat = .arretee
        corpsVisible = false
        squelette.points = [:]
        file.async { [weak self] in
            guard let self else { return }
            if self.session.isRunning {
                self.session.stopRunning()
            }
        }
    }

    private func lancer(exercice: Exercice, objectif: Int) {
        etat = .active
        let numero = serie
        file.async { [weak self] in
            guard let self else { return }
            self.compteur = CompteurRepetitions(exercice: exercice)
            self.serieDeLaFile = numero
            if !self.configuree {
                self.configuree = self.configurer()
            }
            guard self.configuree else {
                DispatchQueue.main.async { self.etat = .indisponible }
                return
            }
            if !self.session.isRunning {
                self.session.startRunning()
            }
        }
    }

    /// Caméra avant, petite définition (suffisante pour la posture), images
    /// livrées droites en mode portrait. Aucune sortie d'enregistrement.
    private func configurer() -> Bool {
        guard let appareil = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front),
              let entree = try? AVCaptureDeviceInput(device: appareil) else {
            return false
        }
        session.beginConfiguration()
        defer { session.commitConfiguration() }
        session.sessionPreset = .vga640x480
        guard session.canAddInput(entree), session.canAddOutput(sortie) else {
            return false
        }
        session.addInput(entree)
        sortie.alwaysDiscardsLateVideoFrames = true
        sortie.setSampleBufferDelegate(self, queue: file)
        session.addOutput(sortie)
        if let connexion = sortie.connection(with: .video), connexion.isVideoOrientationSupported {
            connexion.videoOrientation = .portrait
        }
        // Image en miroir, comme l'aperçu : le squelette dessiné tombe sur le corps.
        if let connexion = sortie.connection(with: .video), connexion.isVideoMirroringSupported {
            connexion.automaticallyAdjustsVideoMirroring = false
            connexion.isVideoMirrored = true
        }
        return true
    }

    /// Détecte la posture sur une image. L'image n'est ni copiée ni gardée.
    private func detecter(_ image: CVPixelBuffer) -> Pose? {
        let traitement = VNImageRequestHandler(cvPixelBuffer: image, orientation: .up, options: [:])
        guard (try? traitement.perform([requete])) != nil,
              let observation = requete.results?.first,
              let reconnus = try? observation.recognizedPoints(.all) else {
            return nil
        }
        // Les positions de Vision vont de 0 à 1 sur chaque axe. On multiplie
        // l'horizontale par le rapport largeur sur hauteur pour garder les angles.
        let largeur = CGFloat(CVPixelBufferGetWidth(image))
        let hauteur = CGFloat(max(CVPixelBufferGetHeight(image), 1))
        let rapport = largeur / hauteur

        var points: [Articulation: CGPoint] = [:]
        for (nom, articulation) in CameraSeance.correspondance {
            if let point = reconnus[nom], point.confidence >= ReglagesComptage.confianceMinimum {
                points[articulation] = CGPoint(x: point.location.x * rapport, y: point.location.y)
            }
        }
        return points.isEmpty ? nil : Pose(points: points)
    }

    private static let correspondance: [(VNHumanBodyPoseObservation.JointName, Articulation)] = [
        (.leftShoulder, .epauleGauche), (.leftElbow, .coudeGauche), (.leftWrist, .poignetGauche),
        (.leftHip, .hancheGauche), (.leftKnee, .genouGauche), (.leftAnkle, .chevilleGauche),
        (.rightShoulder, .epauleDroite), (.rightElbow, .coudeDroit), (.rightWrist, .poignetDroit),
        (.rightHip, .hancheDroite), (.rightKnee, .genouDroit), (.rightAnkle, .chevilleDroite),
    ]

    /// Met à jour ce que l'écran affiche. Toujours sur le fil principal.
    private func publier(
        compte: Int,
        visible: Bool,
        comptee: Bool,
        numero: Int,
        points: [Articulation: CGPoint] = [:],
        rapport: CGFloat = 0
    ) {
        guard etat == .active, numero == serie else { return }
        squelette.rapport = rapport
        if !(points.isEmpty && squelette.points.isEmpty) {
            squelette.points = points
        }
        if corpsVisible != visible {
            corpsVisible = visible
        }
        if repetitions != compte {
            repetitions = compte
        }
        if comptee {
            surRepetition?(compte)
        }
        if objectif > 0, compte >= objectif {
            arreter()
        }
    }

    /// Dans le simulateur, il n'y a pas de caméra : le comptage est simulé pour
    /// pouvoir parcourir les écrans. Ce code n'existe pas sur un vrai iPhone.
    private func simuler(objectif cible: Int) {
        objectif = cible
        etat = .active
        corpsVisible = true
        let numero = serie
        minuterieSimulee?.invalidate()
        minuterieSimulee = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            guard let self else { return }
            self.publier(compte: self.repetitions + 1, visible: true, comptee: true, numero: numero)
        }
    }
}

extension CameraSeance: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard let image = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let pose = detecter(image)
        let points = pose?.points ?? [:]
        let rapport = CGFloat(CVPixelBufferGetWidth(image)) / CGFloat(max(CVPixelBufferGetHeight(image), 1))
        let comptee = compteur.traiter(pose)
        let compte = compteur.compte
        let visible = compteur.corpsVisible
        let numero = serieDeLaFile
        DispatchQueue.main.async { [weak self] in
            self?.publier(
                compte: compte,
                visible: visible,
                comptee: comptee,
                numero: numero,
                points: points,
                rapport: rapport
            )
        }
    }
}
