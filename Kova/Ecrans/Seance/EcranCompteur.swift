import SwiftUI
import UIKit

/// Écran 16 : la séance. Vue caméra, compteur géant, barre à segments.
/// Le bouton de validation n'apparaît qu'à la fin. Abandonner ne donne rien.
struct EcranCompteur: View {
    @EnvironmentObject private var etat: EtatApp
    @ObservedObject var camera: CameraSeance
    let abandonner: () -> Void
    let valider: () -> Void

    private var total: Int { etat.repetitionsDeLaSeance }
    private var faites: Int { min(camera.repetitions, total) }

    /// Segments allumés, au prorata des répétitions faites.
    private var segmentsFaits: Int {
        guard total > 0 else { return 0 }
        return faites * Tokens.Composants.BarreProgression.segments / total
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack {
                BoutonFermer(libelle: L.t("a11y.arreter_seance")) {
                    Analytique.envoyer(.seanceAbandonnee)
                    abandonner()
                }
                Spacer()
                Text(verbatim: L.f("seance.pour_minutes", etat.regle.minutes)).legende(graisse: .moyen)
            }
            VueCamera(camera: camera, hauteur: Tokens.Composants.VueCamera.hauteurSeance)
            CompteurGeant(
                faites: faites,
                total: total,
                tailleTotal: Tokens.TaillesTexte.titreAccueil,
                corpsVisible: camera.corpsVisible
            )
            BarreSegments(faits: segmentsFaits, total: Tokens.Composants.BarreProgression.segments)
        } bas: {
            if faites >= total {
                BoutonPrincipal(L.t("seance.bouton"), action: valider)
                    .entree()
            } else {
                Color.clear.frame(height: Tokens.Hauteurs.boutonPrincipal)
            }
        }
        .onAppear {
            // L'écran reste allumé pendant la séance.
            UIApplication.shared.isIdleTimerDisabled = true
            let objectif = total
            let aVoixHaute = etat.compterAVoixHaute
            camera.surRepetition = { compte in
                if compte >= objectif {
                    Haptique.serieValidee()
                } else {
                    Haptique.repetition()
                }
                if aVoixHaute {
                    Voix.dire(compte)
                }
            }
            camera.demarrer(exercice: etat.exercice, objectif: objectif)
            Analytique.envoyer(.seanceCommencee, ["exercice": etat.exercice.rawValue])
        }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }
}
