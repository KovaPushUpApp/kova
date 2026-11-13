import SwiftUI
import UIKit

/// Écran 16 : la séance. Vue caméra, compteur géant, barre à segments.
/// Le bouton de validation n'apparaît qu'à la fin. Abandonner ne donne rien.
/// À l'étape 2, le comptage est simulé. La caméra arrive à l'étape 3.
struct EcranCompteur: View {
    @EnvironmentObject private var etat: EtatApp
    let abandonner: () -> Void
    let valider: () -> Void

    @State private var faites = 0
    private let minuterie = Timer.publish(every: DonneesFictives.rythmeSeance, on: .main, in: .common).autoconnect()

    private var total: Int { etat.repetitionsDeLaSeance }

    /// Segments allumés, au prorata des répétitions faites.
    private var segmentsFaits: Int {
        guard total > 0 else { return 0 }
        return faites * Tokens.Composants.BarreProgression.segments / total
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack {
                BoutonFermer(libelle: L.t("a11y.arreter_seance"), action: abandonner)
                Spacer()
                Text(verbatim: L.f("seance.pour_minutes", etat.regle.minutes)).legende(graisse: .moyen)
            }
            VueCameraFictive(hauteur: Tokens.Composants.VueCamera.hauteurSeance)
            CompteurGeant(faites: faites, total: total, tailleTotal: Tokens.TaillesTexte.titreAccueil)
            BarreSegments(faits: segmentsFaits, total: Tokens.Composants.BarreProgression.segments)
        } bas: {
            if faites >= total {
                BoutonPrincipal(L.t("seance.bouton"), action: valider)
                    .entree()
            } else {
                Color.clear.frame(height: Tokens.Hauteurs.boutonPrincipal)
            }
        }
        .onReceive(minuterie) { _ in
            guard faites < total else { return }
            faites += 1
            if faites >= total {
                Haptique.serieValidee()
            } else {
                Haptique.repetition()
            }
        }
        // L'écran reste allumé pendant la séance.
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }
}
