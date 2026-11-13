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

/// Le cadre caméra de l'essai et de la séance : aperçu en direct, ligne de scan,
/// et message quand le corps sort du cadre. Sans caméra (refus, simulateur),
/// il montre la silhouette de la maquette.
struct VueCamera: View {
    @ObservedObject var camera: CameraSeance
    let hauteur: CGFloat

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
            // Sur un vrai iPhone, la caméra s'éteint à la fin de la série : pas de légende.
            return CameraSeance.disponible ? "" : L.t("camera.fictive")
        }
    }

    var body: some View {
        let forme = RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous)
        let debut = hauteur * Tokens.Animations.ScanCamera.debut
        let fin = hauteur * Tokens.Animations.ScanCamera.fin
        ZStack(alignment: .top) {
            Tokens.Couleurs.carte
            if enDirect {
                ApercuCamera(session: camera.session)
                    .accessibilityHidden(true)
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
            if enDirect && !camera.corpsVisible {
                Text(verbatim: L.t("camera.hors_cadre"))
                    .corps(Tokens.Couleurs.texteSurAccent, graisse: .demiGras)
                    .padding(.horizontal, Tokens.Espacements.e13)
                    .padding(.vertical, Tokens.Espacements.e8)
                    .background(Tokens.Couleurs.accent)
                    .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
                    .padding(.top, Tokens.Espacements.e13)
            }
        }
        .frame(height: hauteur)
        .frame(maxWidth: .infinity)
        .clipShape(forme)
        .overlay(
            forme.stroke(
                Tokens.Couleurs.pointille,
                style: StrokeStyle(lineWidth: Tokens.Traits.fin, dash: [Tokens.Espacements.e5, Tokens.Espacements.e5])
            )
        )
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
}
