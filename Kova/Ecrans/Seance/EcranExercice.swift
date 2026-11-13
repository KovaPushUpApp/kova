import SwiftUI

/// Écran 14 : choix de l'exercice, avec le nombre de répétitions demandé.
/// Seuls les exercices dont le comptage est livré sont proposés.
struct EcranExercice: View {
    @EnvironmentObject private var etat: EtatApp
    let retour: () -> Void
    let commencer: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(spacing: Tokens.Espacements.e13) {
                BoutonRetour(action: retour)
                Text(verbatim: L.f("seance.pour_minutes", etat.regle.minutes)).legende()
            }
            Text(verbatim: L.t("exercice.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            VStack(spacing: Tokens.Espacements.e8) {
                ForEach(Exercice.disponibles, id: \.self) { exercice in
                    CarteOption(choisie: etat.exercice == exercice, action: { etat.exercice = exercice }) {
                        HStack {
                            Text(verbatim: exercice.nom).corps(graisse: .demiGras)
                            Spacer(minLength: Tokens.Espacements.e8)
                            Text(verbatim: String(etat.regle.repetitionsDemandees(pour: exercice)))
                                .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                                .foregroundColor(
                                    etat.exercice == exercice ? Tokens.Couleurs.accent : Tokens.Couleurs.texteSecondaire
                                )
                        }
                    }
                }
            }
            .padding(.top, Tokens.Espacements.e8)
            .entree(rang: 2)
        } bas: {
            BoutonPrincipal(L.t("exercice.bouton"), action: commencer)
                .entree(rang: 4)
        }
    }
}
