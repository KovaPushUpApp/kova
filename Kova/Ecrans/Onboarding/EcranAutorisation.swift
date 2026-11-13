import SwiftUI

/// Écrans 7 et 8 : explication puis demande d'une autorisation.
/// À l'étape 2, le bouton passe à la suite. La vraie demande arrive
/// à l'étape 3 (caméra) et à l'étape 4 (Temps d'écran).
struct EcranAutorisation: View {
    let etape: Int
    let icone: Icone.Nom
    let titre: String
    let texte: String
    let arguments: [String]
    let bouton: String
    let retour: () -> Void
    let autoriser: () -> Void

    var body: some View {
        EcranKova {
            EnTeteEtapes(etape: etape, total: EtapeOnboarding.nombreEtapes, retour: retour)
            IconeAnneau(icone: icone)
            Text(verbatim: titre)
                .titreEcran()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 2)
            Text(verbatim: texte)
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 3)
            VStack(alignment: .leading, spacing: Tokens.Espacements.e13) {
                ForEach(arguments, id: \.self) { argument in
                    LigneCoche(texte: argument)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .carte()
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 4)
        } bas: {
            BoutonPrincipal(bouton, action: autoriser)
                .entree(rang: 6)
        }
    }
}

extension EcranAutorisation {
    /// Écran 7 : accès Temps d'écran.
    static func tempsDEcran(retour: @escaping () -> Void, autoriser: @escaping () -> Void) -> EcranAutorisation {
        EcranAutorisation(
            etape: 6,
            icone: .cadenas,
            titre: L.t("auto_ecran.titre"),
            texte: L.t("auto_ecran.texte"),
            arguments: [L.t("auto_ecran.argument.1"), L.t("auto_ecran.argument.2"), L.t("auto_ecran.argument.3")],
            bouton: L.t("auto_ecran.bouton"),
            retour: retour,
            autoriser: autoriser
        )
    }

    /// Écran 8 : accès caméra.
    static func camera(retour: @escaping () -> Void, autoriser: @escaping () -> Void) -> EcranAutorisation {
        EcranAutorisation(
            etape: 7,
            icone: .camera,
            titre: L.t("auto_camera.titre"),
            texte: L.t("auto_camera.texte"),
            arguments: [L.t("auto_camera.argument.1"), L.t("auto_camera.argument.2"), L.t("auto_camera.argument.3")],
            bouton: L.t("auto_camera.bouton"),
            retour: retour,
            autoriser: autoriser
        )
    }
}
