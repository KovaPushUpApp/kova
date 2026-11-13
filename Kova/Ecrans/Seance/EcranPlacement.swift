import SwiftUI

/// Écran 15 : mise en place du téléphone. La caméra tourne déjà : le bouton
/// apparaît quand le corps est détecté. La caméra est gérée par la navigation,
/// qui l'éteint quand on quitte la séance.
struct EcranPlacement: View {
    @EnvironmentObject private var etat: EtatApp
    @ObservedObject var camera: CameraSeance
    let retour: () -> Void
    let suivant: () -> Void

    /// Inclinaison du téléphone dessiné, reprise de la maquette.
    private let inclinaison = Angle.degrees(-12)

    private var cameraRefusee: Bool {
        camera.etat == .refusee || camera.etat == .indisponible
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
            illustration
                .padding(.top, Tokens.Espacements.e8)
                .entree(rang: 3)
            if camera.corpsVisible {
                LigneCoche(texte: L.t("placement.detecte"))
                    .entree()
            } else if cameraRefusee {
                Text(verbatim: L.t("camera.refusee")).paragraphe()
            }
        } bas: {
            if camera.corpsVisible {
                BoutonPrincipal(L.t("placement.bouton"), action: suivant)
                    .entree()
            } else if cameraRefusee {
                BoutonPrincipal(L.t("camera.ouvrir_reglages"), action: ouvrirReglagesSysteme)
            } else {
                Color.clear.frame(height: Tokens.Hauteurs.boutonPrincipal)
            }
        }
        .onAppear {
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
