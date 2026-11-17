import SwiftUI
import UIKit

/// Écran 16 : la séance. Vue caméra, compteur géant, barre à segments.
/// Le bouton de validation apparaît quand la série de la règle est faite. On
/// peut continuer : chaque répétition en plus ajoute du temps, et c'est le
/// bouton qui termine la séance. Abandonner ne donne rien.
struct EcranCompteur: View {
    @EnvironmentObject private var etat: EtatApp
    @ObservedObject var camera: CameraSeance
    let abandonner: () -> Void
    /// Reçoit les répétitions vraiment faites.
    let valider: (Int) -> Void

    private var total: Int { etat.repetitionsDeLaSeance }
    private var faites: Int { camera.repetitions }
    /// Minutes gagnées si la séance s'arrête maintenant.
    private var gagnees: Int { etat.regle.minutesPour(etat.exercice, faites) }

    /// Segments allumés, au prorata des répétitions faites. La barre est pleine à la série.
    private var segmentsFaits: Int {
        guard total > 0 else { return 0 }
        let segments = Tokens.Composants.BarreProgression.segments
        return min(segments, faites * segments / total)
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack {
                BoutonFermer(libelle: L.t("a11y.arreter_seance")) {
                    Analytique.envoyer(.seanceAbandonnee)
                    abandonner()
                }
                Spacer()
                Text(verbatim: gagnees > 0
                    ? L.f("seance.gagnees", gagnees)
                    : L.f("seance.pour_minutes", etat.regle.minutes))
                    .legende(graisse: .moyen)
            }
            VueCamera(camera: camera, hauteur: Tokens.Composants.VueCamera.hauteurSeance)
            CompteurGeant(
                faites: faites,
                total: total,
                tailleTotal: Tokens.TaillesTexte.titreAccueil,
                corpsVisible: camera.corpsVisible,
                messageValide: L.t("seance.continuer")
            )
            BarreSegments(faits: segmentsFaits, total: Tokens.Composants.BarreProgression.segments)
        } bas: {
            if faites >= total {
                BoutonPrincipal(L.f("seance.bouton.minutes", gagnees)) {
                    valider(faites)
                }
                .entree()
            } else {
                Color.clear.frame(height: Tokens.Hauteurs.boutonPrincipal)
            }
        }
        .onAppear {
            // L'écran reste allumé pendant la séance.
            UIApplication.shared.isIdleTimerDisabled = true
            let serie = total
            let aVoixHaute = etat.compterAVoixHaute
            camera.surRepetition = { compte in
                // Vibration forte à chaque série complète.
                if serie > 0, compte % serie == 0 {
                    Haptique.serieValidee()
                } else {
                    Haptique.repetition()
                }
                if aVoixHaute {
                    Voix.dire(compte)
                }
            }
            // Objectif 0 : la caméra ne s'éteint pas à la série, la séance est libre.
            camera.demarrer(exercice: etat.exercice, objectif: 0)
            Analytique.envoyer(.seanceCommencee, ["exercice": etat.exercice.rawValue])
        }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }
}
