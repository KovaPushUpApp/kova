import SwiftUI
import UIKit

/// Message d'encouragement commun à l'essai et à la séance. `valide` est le
/// message affiché une fois la série faite.
func messageDeSeance(faites: Int, total: Int, corpsVisible: Bool, valide: String) -> String {
    if faites >= total {
        return valide
    }
    if !corpsVisible || faites == 0 {
        return L.t("seance.en_position")
    }
    return L.f("seance.encore", total - faites)
}

/// Compteur géant "3/10" de l'essai et de la séance.
struct CompteurGeant: View {
    let faites: Int
    let total: Int
    let tailleTotal: CGFloat
    var corpsVisible = true
    /// Message affiché une fois la série faite.
    var messageValide = L.t("seance.validee")

    var body: some View {
        VStack(spacing: Tokens.Espacements.e3) {
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text(verbatim: String(faites))
                    .font(.kovaChiffre(Tokens.TaillesTexte.chiffreGeant))
                    .foregroundColor(Tokens.Couleurs.texte)
                    .monospacedDigit()
                Text(verbatim: "/" + String(total))
                    .font(.kovaChiffre(tailleTotal))
                    .foregroundColor(Tokens.Couleurs.texteSecondaire)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: L.f("a11y.progression_valeur", faites, total)))
            Text(verbatim: messageDeSeance(
                faites: faites,
                total: total,
                corpsVisible: corpsVisible,
                valide: messageValide
            ))
                .corps(Tokens.Couleurs.texteSecondaire)
        }
        .frame(maxWidth: .infinity)
    }
}

/// Ouvre la page de Kova dans les Réglages du téléphone.
func ouvrirReglagesSysteme() {
    if let lien = URL(string: UIApplication.openSettingsURLString) {
        UIApplication.shared.open(lien)
    }
}

/// Écran 9 : le moment de preuve. Trois vraies pompes comptées en direct,
/// puis le bouton "Continuer" apparaît. La caméra remplit l'écran, comme
/// pendant la séance.
struct EcranEssai: View {
    let retour: () -> Void
    /// Reçoit vrai si les 3 pompes ont été faites, faux si l'essai a été passé.
    let suivant: (Bool) -> Void

    @StateObject private var camera = CameraSeance()
    private let total = 3

    private var cameraRefusee: Bool {
        camera.etat == .refusee || camera.etat == .indisponible
    }

    var body: some View {
        EcranCamera(camera: camera) {
            EnTeteEtapes(etape: 8, total: EtapeOnboarding.nombreEtapes, retour: retour)
        } bas: {
            Text(verbatim: L.t("essai.titre"))
                .titreCarte()
                .multilineTextAlignment(.center)
            if camera.repetitions == 0 {
                Text(verbatim: L.t("essai.conseil"))
                    .legende(Tokens.Couleurs.texte)
                    .multilineTextAlignment(.center)
            }
            CompteurGeant(
                faites: camera.repetitions,
                total: total,
                tailleTotal: Tokens.TaillesTexte.titreEcran,
                corpsVisible: camera.corpsVisible
            )
            Text(verbatim: L.t("sante.avertissement"))
                .legende()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .fixedSize(horizontal: false, vertical: true)
            if camera.repetitions >= total {
                BoutonPrincipal(L.t("essai.bouton")) {
                    Analytique.envoyer(.essaiReussi)
                    suivant(true)
                }
                .entree()
            } else if cameraRefusee {
                // Sans caméra, l'essai est impossible : on ne bloque pas l'onboarding.
                BoutonPrincipal(L.t("camera.ouvrir_reglages"), action: ouvrirReglagesSysteme)
                BoutonSecondaire(L.t("essai.passer")) { suivant(false) }
            } else {
                // On peut toujours passer : personne ne doit rester coincé ici,
                // ni quelqu'un qui ne peut pas faire de pompes, ni la revue d'Apple.
                BoutonSecondaire(L.t("essai.passer")) { suivant(false) }
            }
        }
        .onAppear {
            let objectif = total
            camera.surRepetition = { compte in
                if compte >= objectif {
                    Haptique.serieValidee()
                } else {
                    Haptique.repetition()
                }
            }
            camera.demarrer(exercice: .pompes, objectif: total)
        }
        .onDisappear { camera.arreter() }
    }
}
