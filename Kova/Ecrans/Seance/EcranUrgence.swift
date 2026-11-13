import SwiftUI

/// Écran 18 : déblocage d'urgence. 5 minutes sans exercice, 2 fois par semaine.
struct EcranUrgence: View {
    let retour: () -> Void
    let debloquer: () -> Void
    let faireSesPompes: () -> Void

    var body: some View {
        EcranKova {
            BoutonRetour(action: retour)
            IconeAnneau(icone: .eclair, avecAnneau: false)
            Text(verbatim: L.t("urgence.titre"))
                .titreEcran()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 2)
            Text(verbatim: L.f("urgence.texte", DonneesFictives.minutesUrgence))
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 3)
            HStack {
                Text(verbatim: L.t("urgence.restant")).corps()
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: L.f("urgence.compte", DonneesFictives.urgencesRestantes, DonneesFictives.urgencesParSemaine))
                    .font(.kovaTitre(Tokens.TaillesTexte.titreCarte, relatifA: .title2))
                    .foregroundColor(Tokens.Couleurs.accent)
            }
            .carte()
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 4)
        } bas: {
            BoutonPrincipal(L.f("urgence.bouton", DonneesFictives.minutesUrgence), style: .contour, action: debloquer)
                .entree(rang: 6)
            BoutonSecondaire(L.t("urgence.pompes"), couleur: Tokens.Couleurs.texte, action: faireSesPompes)
        }
    }
}
