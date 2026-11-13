import SwiftUI

/// Écran 18 : déblocage d'urgence. 5 minutes sans exercice, 2 fois par semaine,
/// remise à zéro le lundi (brief, règle 6). Passe même pendant une plage stricte.
struct EcranUrgence: View {
    @EnvironmentObject private var etat: EtatApp
    let retour: () -> Void
    let debloquer: () -> Void
    let faireSesPompes: () -> Void

    private var restantes: Int {
        etat.jeu.urgencesRestantes(Date(), etat.calendrier)
    }

    private var disponible: Bool {
        etat.jeu.urgenceActivee && restantes > 0
    }

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
            Text(verbatim: L.f("urgence.texte", EtatJeu.minutesUrgence))
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 3)
            HStack {
                Text(verbatim: L.t("urgence.restant")).corps()
                Spacer(minLength: Tokens.Espacements.e8)
                Text(verbatim: L.f("urgence.compte", restantes, EtatJeu.urgencesParSemaine))
                    .font(.kovaTitre(Tokens.TaillesTexte.titreCarte, relatifA: .title2))
                    .foregroundColor(disponible ? Tokens.Couleurs.accent : Tokens.Couleurs.texteSecondaire)
            }
            .carte()
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 4)
            if !disponible {
                Text(verbatim: L.t(etat.jeu.urgenceActivee ? "urgence.epuisee" : "urgence.desactivee"))
                    .paragraphe()
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
        } bas: {
            if disponible {
                BoutonPrincipal(L.f("urgence.bouton", EtatJeu.minutesUrgence), style: .contour, action: debloquer)
                    .entree(rang: 6)
                BoutonSecondaire(L.t("urgence.pompes"), couleur: Tokens.Couleurs.texte, action: faireSesPompes)
            } else {
                BoutonPrincipal(L.t("urgence.pompes"), action: faireSesPompes)
                    .entree(rang: 6)
            }
        }
    }
}
