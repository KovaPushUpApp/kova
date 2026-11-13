import SwiftUI

/// Écrans d'autorisation : une explication, puis la demande du système.
/// En cas de refus, l'écran explique pourquoi l'accès est nécessaire et
/// propose de réessayer ou d'ouvrir les Réglages (brief, section 7).
struct EcranAutorisation: View {
    let etape: Int
    let icone: Icone.Nom
    let titre: String
    let texte: String
    let arguments: [String]
    let bouton: String
    /// Message affiché après un refus. Nil tant que rien n'a été refusé.
    var refus: String?
    var enCours = false
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
            if let refus {
                Text(verbatim: refus)
                    .paragraphe(Tokens.Couleurs.accent)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        } bas: {
            BoutonPrincipal(refus == nil ? bouton : L.t("autorisation.reessayer"), action: autoriser)
                .opacity(enCours ? Double(Tokens.Opacites.scan) : 1)
                .disabled(enCours)
                .entree(rang: 6)
            if refus != nil {
                BoutonSecondaire(L.t("camera.ouvrir_reglages"), action: ouvrirReglagesSysteme)
            }
        }
    }
}

extension EcranAutorisation {
    /// Accès Temps d'écran. On ne peut pas continuer sans.
    static func tempsDEcran(
        refuse: Bool,
        enCours: Bool,
        retour: @escaping () -> Void,
        autoriser: @escaping () -> Void
    ) -> EcranAutorisation {
        EcranAutorisation(
            etape: 4,
            icone: .cadenas,
            titre: L.t("auto_ecran.titre"),
            texte: L.t("auto_ecran.texte"),
            arguments: [L.t("auto_ecran.argument.1"), L.t("auto_ecran.argument.2"), L.t("auto_ecran.argument.3")],
            bouton: L.t("auto_ecran.bouton"),
            refus: refuse ? L.t("auto_ecran.refus") : nil,
            enCours: enCours,
            retour: retour,
            autoriser: autoriser
        )
    }

    /// Accès caméra.
    static func camera(
        refuse: Bool,
        retour: @escaping () -> Void,
        autoriser: @escaping () -> Void
    ) -> EcranAutorisation {
        EcranAutorisation(
            etape: 7,
            icone: .camera,
            titre: L.t("auto_camera.titre"),
            texte: L.t("auto_camera.texte"),
            arguments: [L.t("auto_camera.argument.1"), L.t("auto_camera.argument.2"), L.t("auto_camera.argument.3")],
            bouton: L.t("auto_camera.bouton"),
            refus: refuse ? L.t("auto_camera.refus") : nil,
            retour: retour,
            autoriser: autoriser
        )
    }
}
