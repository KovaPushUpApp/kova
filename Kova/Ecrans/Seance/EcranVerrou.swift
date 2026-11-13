import SwiftUI

/// Écran 13, version dans l'app. Le vrai écran de verrou est dessiné par le système
/// par dessus l'app bloquée, avec un titre, un sous-titre et deux boutons au plus
/// (extension KovaShieldConfig). Celui-ci reprend la maquette : c'est l'écran sur
/// lequel on arrive dans Kova après avoir touché "Faire mes pompes".
/// Le nom de l'app bloquée n'est pas connu de Kova, d'où "Tes apps".
struct EcranVerrou: View {
    @EnvironmentObject private var etat: EtatApp
    let commencer: () -> Void
    let fermer: () -> Void
    let urgence: () -> Void

    var body: some View {
        EcranKova {
            IconeAnneau(icone: .cadenas)
            Text(verbatim: L.t("verrou.app.titre"))
                .titreEcran()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 2)
            Text(verbatim: L.t("verrou.app.texte"))
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 3)
            HStack {
                bloc(
                    L.f("verrou.app.pompes", etat.regle.repetitions),
                    L.t("verrou.app.a_faire"),
                    couleur: Tokens.Couleurs.texte,
                    alignement: .leading
                )
                Spacer(minLength: Tokens.Espacements.e8)
                Icone(nom: .fleche, couleur: Tokens.Couleurs.texteSecondaire)
                Spacer(minLength: Tokens.Espacements.e8)
                bloc(
                    L.f("commun.minutes_court", etat.regle.minutes),
                    L.t("verrou.app.debloquees"),
                    couleur: Tokens.Couleurs.accent,
                    alignement: .trailing
                )
            }
            .carte()
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 4)
        } bas: {
            BoutonPrincipal(L.t("verrou.app.commencer"), action: commencer)
                .entree(rang: 6)
            BoutonSecondaire(L.t("verrou.app.plus_tard"), action: fermer)
            BoutonSecondaire(L.t("ecran.urgence"), action: urgence)
        }
    }

    private func bloc(
        _ valeur: String,
        _ libelle: String,
        couleur: Color,
        alignement: HorizontalAlignment
    ) -> some View {
        VStack(alignment: alignement, spacing: Tokens.Espacements.e3) {
            Text(verbatim: valeur)
                .font(.kovaTitre(Tokens.TaillesTexte.titreEcran))
                .foregroundColor(couleur)
                .lineLimit(1)
                .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
            Text(verbatim: libelle).legende()
        }
    }
}
