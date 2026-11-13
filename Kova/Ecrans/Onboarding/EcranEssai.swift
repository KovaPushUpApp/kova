import SwiftUI
import UIKit

/// Message d'encouragement commun à l'essai et à la séance.
func messageDeSeance(faites: Int, total: Int, corpsVisible: Bool) -> String {
    if faites >= total {
        return L.t("seance.validee")
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
            Text(verbatim: messageDeSeance(faites: faites, total: total, corpsVisible: corpsVisible))
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
/// puis le bouton "Continuer" apparaît.
struct EcranEssai: View {
    let retour: () -> Void
    let suivant: () -> Void

    @StateObject private var camera = CameraSeance()
    private let total = 3

    private var cameraRefusee: Bool {
        camera.etat == .refusee || camera.etat == .indisponible
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            EnTeteEtapes(etape: 8, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.t("essai.titre"))
                .titreCarte()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            VueCamera(camera: camera, hauteur: Tokens.Composants.VueCamera.hauteurEssai)
            CompteurGeant(
                faites: camera.repetitions,
                total: total,
                tailleTotal: Tokens.TaillesTexte.titreEcran,
                corpsVisible: camera.corpsVisible
            )
        } bas: {
            if camera.repetitions >= total {
                BoutonPrincipal(L.t("essai.bouton")) {
                    Analytique.envoyer(.essaiReussi)
                    suivant()
                }
                .entree()
            } else if cameraRefusee {
                // Sans caméra, l'essai est impossible : on ne bloque pas l'onboarding.
                BoutonPrincipal(L.t("camera.ouvrir_reglages"), action: ouvrirReglagesSysteme)
                BoutonSecondaire(L.t("essai.passer"), action: suivant)
            } else {
                Color.clear.frame(height: Tokens.Hauteurs.boutonPrincipal)
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
