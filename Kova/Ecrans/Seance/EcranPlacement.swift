import SwiftUI

/// Écran 15 : mise en place du téléphone. Les consignes restent en haut et la
/// caméra s'affiche tout de suite dans le cadre, pour se voir et se placer.
/// Dès que le corps est détecté, la séance s'ouvre toute seule en plein écran :
/// pas besoin de revenir toucher le téléphone. Le bouton reste là pour passer
/// à la main. La caméra est gérée par la navigation, qui l'éteint quand on
/// quitte la séance.
struct EcranPlacement: View {
    @EnvironmentObject private var etat: EtatApp
    @ObservedObject var camera: CameraSeance
    let retour: () -> Void
    let suivant: () -> Void

    /// Inclinaison du téléphone dessiné, reprise de la maquette.
    private let inclinaison = Angle.degrees(-12)

    /// Vrai une fois la séance ouverte, pour ne pas l'ouvrir deux fois.
    @State private var parti = false

    /// Temps de détection continue avant d'ouvrir la séance, en secondes.
    private let attenteAvantDepart = 1.5

    private var cameraRefusee: Bool {
        camera.etat == .refusee || camera.etat == .indisponible
    }

    private var enDirect: Bool {
        camera.etat == .active && CameraSeance.disponible
    }

    private func partir() {
        guard !parti else { return }
        parti = true
        suivant()
    }

    var body: some View {
        EcranKova {
            BoutonRetour(action: retour)
            Text(verbatim: L.t("placement.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            Text(verbatim: L.t("placement.texte"))
                .paragraphe()
                .entree(rang: 2)
            Group {
                if enDirect {
                    VueCamera(camera: camera, hauteur: Tokens.Composants.VueCamera.hauteurSeance)
                } else {
                    illustration
                }
            }
            .padding(.top, Tokens.Espacements.e8)
            .entree(rang: 3)
            if camera.corpsVisible {
                LigneCoche(texte: L.t("placement.detecte"))
                    .entree()
            } else if cameraRefusee {
                Text(verbatim: L.t("camera.refusee")).paragraphe()
            }
        } bas: {
            if cameraRefusee {
                BoutonPrincipal(L.t("camera.ouvrir_reglages"), action: ouvrirReglagesSysteme)
            } else {
                // Toujours actif : on peut lancer la séance même si la détection tarde.
                BoutonPrincipal(L.t("placement.bouton"), action: partir)
            }
        }
        .task(id: camera.corpsVisible) {
            guard camera.corpsVisible else { return }
            try? await Task.sleep(nanoseconds: UInt64(attenteAvantDepart * 1_000_000_000))
            if !Task.isCancelled {
                partir()
            }
        }
        .onAppear {
            parti = false
            // Objectif à zéro : la caméra observe sans s'arrêter seule.
            camera.surRepetition = nil
            camera.demarrer(exercice: etat.exercice, objectif: 0)
        }
    }

    private var illustration: some View {
        VStack(spacing: Tokens.Espacements.e13) {
            Spacer(minLength: 0)
            HStack(alignment: .bottom, spacing: Tokens.Espacements.e13) {
                ZStack {
                    Anneau()
                        .frame(width: Tokens.Espacements.e55, height: Tokens.Espacements.e55)
                    RoundedRectangle(cornerRadius: Tokens.Arrondis.petit, style: .continuous)
                        .stroke(Tokens.Couleurs.accent, lineWidth: Tokens.Traits.illustration)
                        .frame(width: Tokens.Espacements.e34, height: Tokens.Espacements.e55)
                        .rotationEffect(inclinaison)
                }
                .frame(width: Tokens.Espacements.e55, height: Tokens.Espacements.e55)
                Rectangle()
                    .stroke(
                        Tokens.Couleurs.pointille,
                        style: StrokeStyle(
                            lineWidth: Tokens.Traits.normal,
                            dash: [Tokens.Espacements.e5, Tokens.Espacements.e5]
                        )
                    )
                    .frame(width: Tokens.Espacements.e34, height: Tokens.Traits.normal)
                    .padding(.bottom, Tokens.Espacements.e8)
                FigurePompe(animee: false)
            }
            Text(verbatim: L.t("placement.distance")).legende()
        }
        .padding(.bottom, Tokens.Espacements.e21)
        .frame(maxWidth: .infinity)
        .frame(height: Tokens.Composants.VueCamera.hauteurEssai)
        .background(Tokens.Couleurs.carte)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: L.t("a11y.placement")))
    }
}
