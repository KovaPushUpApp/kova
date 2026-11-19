import AVFoundation
import SwiftUI
import UIKit

/// Aperçu en direct de la caméra. L'image s'affiche, elle n'est pas enregistrée.
private struct ApercuCamera: UIViewRepresentable {
    let session: AVCaptureSession

    final class Vue: UIView {
        override class var layerClass: AnyClass {
            AVCaptureVideoPreviewLayer.self
        }

        var couche: AVCaptureVideoPreviewLayer? {
            layer as? AVCaptureVideoPreviewLayer
        }
    }

    func makeUIView(context: Context) -> Vue {
        let vue = Vue()
        vue.couche?.session = session
        vue.couche?.videoGravity = .resizeAspectFill
        return vue
    }

    func updateUIView(_ uiView: Vue, context: Context) {}
}

/// Le squelette détecté, dessiné par dessus l'aperçu. Il montre ce que Kova
/// voit : si le squelette te suit, le comptage peut marcher. L'aperçu remplit
/// le cadre en rognant l'image : les positions sont converties de la même façon.
private struct Squelette: View {
    @ObservedObject var squelette: SqueletteCamera

    /// Les segments dessinés : bras, tronc et jambes.
    private static let segments: [(Articulation, Articulation)] = [
        (.epauleGauche, .epauleDroite), (.hancheGauche, .hancheDroite),
        (.epauleGauche, .coudeGauche), (.coudeGauche, .poignetGauche),
        (.epauleDroite, .coudeDroit), (.coudeDroit, .poignetDroit),
        (.epauleGauche, .hancheGauche), (.epauleDroite, .hancheDroite),
        (.hancheGauche, .genouGauche), (.genouGauche, .chevilleGauche),
        (.hancheDroite, .genouDroit), (.genouDroit, .chevilleDroite),
    ]

    var body: some View {
        let points = squelette.points
        let rapport = squelette.rapport
        Canvas { contexte, taille in
            guard !points.isEmpty, rapport > 0 else { return }
            // Une unité de la pose vaut la hauteur de l'image. L'image remplit le cadre.
            let echelle = max(taille.width / rapport, taille.height)
            let decalageX = (taille.width - rapport * echelle) / 2
            let decalageY = (taille.height - echelle) / 2
            // La verticale de la détection part du bas, celle de l'écran du haut.
            let place: (CGPoint) -> CGPoint = { point in
                CGPoint(x: decalageX + point.x * echelle, y: decalageY + (1 - point.y) * echelle)
            }
            let trait = Tokens.Traits.illustration
            for (debut, fin) in Squelette.segments {
                guard let a = points[debut], let b = points[fin] else { continue }
                var chemin = Path()
                chemin.move(to: place(a))
                chemin.addLine(to: place(b))
                contexte.stroke(
                    chemin,
                    with: .color(Tokens.Couleurs.accent),
                    style: StrokeStyle(lineWidth: trait, lineCap: .round)
                )
            }
            for point in points.values {
                let centre = place(point)
                let rond = CGRect(x: centre.x - trait, y: centre.y - trait, width: trait * 2, height: trait * 2)
                contexte.fill(Path(ellipseIn: rond), with: .color(Tokens.Couleurs.texte))
            }
        }
        .accessibilityHidden(true)
    }
}

/// Petite étiquette lisible par dessus l'image de la caméra.
struct EtiquetteCamera: View {
    let texte: String
    var accent = false

    var body: some View {
        Text(verbatim: texte)
            .corps(accent ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texte, graisse: .demiGras)
            .padding(.horizontal, Tokens.Espacements.e13)
            .padding(.vertical, Tokens.Espacements.e8)
            .background(
                accent
                    ? Tokens.Couleurs.accent
                    : Tokens.Couleurs.fond.opacity(Double(Tokens.Opacites.voileCamera))
            )
            .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
    }
}

/// Le retour caméra : aperçu en direct, squelette par dessus et ligne de scan.
/// Avec une hauteur, c'est un cadre arrondi. Sans hauteur, il remplit toute la
/// place disponible. Sans caméra (refus, simulateur), il montre la silhouette
/// de la maquette.
struct VueCamera: View {
    @ObservedObject var camera: CameraSeance
    var hauteur: CGFloat?

    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var enBas = false

    private var enDirect: Bool {
        camera.etat == .active && CameraSeance.disponible
    }

    private var message: String {
        switch camera.etat {
        case .refusee: return L.t("camera.refusee")
        case .indisponible: return L.t("camera.indisponible")
        case .active, .arretee:
            return CameraSeance.disponible ? "" : L.t("camera.fictive")
        }
    }

    var body: some View {
        Group {
            if let hauteur {
                let forme = RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous)
                contenu
                    .frame(height: hauteur)
                    .frame(maxWidth: .infinity)
                    .clipShape(forme)
                    .overlay(
                        forme.stroke(
                            Tokens.Couleurs.pointille,
                            style: StrokeStyle(
                                lineWidth: Tokens.Traits.fin,
                                dash: [Tokens.Espacements.e5, Tokens.Espacements.e5]
                            )
                        )
                    )
            } else {
                contenu
            }
        }
        .onAppear {
            guard !reduire else { return }
            withAnimation(
                .linear(duration: Double(Tokens.Animations.ScanCamera.duree) / 2)
                    .repeatForever(autoreverses: true)
            ) {
                enBas = true
            }
        }
    }

    private var contenu: some View {
        GeometryReader { geometrie in
            let debut = geometrie.size.height * Tokens.Animations.ScanCamera.debut
            let fin = geometrie.size.height * Tokens.Animations.ScanCamera.fin
            ZStack(alignment: .top) {
                Tokens.Couleurs.carte
                if enDirect {
                    ApercuCamera(session: camera.session)
                        .accessibilityHidden(true)
                    Squelette(squelette: camera.squelette)
                } else {
                    VStack(spacing: Tokens.Espacements.e13) {
                        FigurePompe()
                        Text(verbatim: message)
                            .legende()
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, Tokens.Espacements.e21)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                Rectangle()
                    .fill(Tokens.Couleurs.accent)
                    .opacity(Double(Tokens.Opacites.scan))
                    .frame(height: Tokens.Traits.normal)
                    .offset(y: enBas ? fin : debut)
                    .opacity(reduire ? 0 : 1)
                    .accessibilityHidden(true)
                // En plein écran, c'est le gabarit qui affiche ce message, au centre.
                if hauteur != nil && enDirect && !camera.corpsVisible {
                    EtiquetteCamera(texte: L.t("camera.hors_cadre"), accent: true)
                        .padding(.top, Tokens.Espacements.e13)
                }
            }
        }
    }
}

/// Gabarit des écrans de comptage : le retour caméra remplit tout l'écran, pour
/// bien se voir. L'en-tête est en haut, le compteur et le bouton en bas, sur un
/// voile sombre qui les garde lisibles. Écart voulu avec la maquette, où la
/// caméra n'occupe que 38 % de la hauteur. Même gabarit que sur Android.
struct EcranCamera<Haut: View, Bas: View>: View {
    @ObservedObject private var camera: CameraSeance
    private let haut: Haut
    private let bas: Bas

    init(camera: CameraSeance, @ViewBuilder haut: () -> Haut, @ViewBuilder bas: () -> Bas) {
        _camera = ObservedObject(wrappedValue: camera)
        self.haut = haut()
        self.bas = bas()
    }

    private var enDirect: Bool {
        camera.etat == .active && CameraSeance.disponible
    }

    private var voile: LinearGradient {
        let fond = Tokens.Couleurs.fond
        return LinearGradient(
            stops: [
                .init(color: fond.opacity(0), location: 0),
                .init(
                    color: fond.opacity(Double(Tokens.Opacites.voileCamera)),
                    location: Tokens.Composants.VueCamera.partDeLecran
                ),
                .init(color: fond, location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    var body: some View {
        ZStack {
            VueCamera(camera: camera)
                .ignoresSafeArea()
            if enDirect && !camera.corpsVisible {
                EtiquetteCamera(texte: L.t("camera.hors_cadre"), accent: true)
            }
            VStack(spacing: 0) {
                HStack(spacing: Tokens.Espacements.e13) {
                    haut
                }
                .padding(.horizontal, Tokens.Ecran.margeCotes)
                .padding(.vertical, Tokens.Espacements.e8)
                Spacer(minLength: 0)
                VStack(spacing: Tokens.Espacements.e13) {
                    bas
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, Tokens.Ecran.margeCotes)
                .padding(.top, Tokens.Espacements.e55)
                .padding(.bottom, Tokens.Espacements.e8)
                .background(voile.ignoresSafeArea(edges: .bottom))
            }
        }
        .background(Tokens.Couleurs.fond.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}
