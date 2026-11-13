import SwiftUI

/// Écran 14 : choix de l'exercice, avec le nombre de répétitions demandé.
/// Seuls les exercices dont le comptage est livré sont proposés. Sans
/// abonnement, seules les pompes sont utilisables (brief, section 4).
/// Sert aussi au réglage "Exercice par défaut".
struct EcranExercice: View {
    enum Contexte {
        case seance, parDefaut
    }

    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var abonnements: Abonnements
    let contexte: Contexte
    let retour: () -> Void
    let valider: () -> Void
    let ouvrirPaywall: () -> Void

    private var choisi: Exercice {
        contexte == .seance ? etat.exercice : etat.jeu.exerciceParDefaut
    }

    private func reserve(_ exercice: Exercice) -> Bool {
        !abonnements.estAbonne && !Exercice.gratuits.contains(exercice)
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(spacing: Tokens.Espacements.e13) {
                BoutonRetour(action: retour)
                Text(verbatim: L.f("seance.pour_minutes", etat.regle.minutes)).legende()
            }
            Text(verbatim: L.t(contexte == .seance ? "exercice.titre" : "reglages.exercice"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            VStack(spacing: Tokens.Espacements.e8) {
                ForEach(Exercice.disponibles, id: \.self) { exercice in
                    CarteOption(choisie: choisi == exercice, action: { choisir(exercice) }) {
                        HStack(spacing: Tokens.Espacements.e8) {
                            Text(verbatim: exercice.nom).corps(graisse: .demiGras)
                            if reserve(exercice) {
                                Icone(nom: .cadenas, taille: Tokens.Espacements.e13, couleur: Tokens.Couleurs.texteSecondaire)
                            }
                            Spacer(minLength: Tokens.Espacements.e8)
                            Text(verbatim: String(etat.regle.repetitionsDemandees(pour: exercice)))
                                .font(.kovaTitre(Tokens.TaillesTexte.corpsFort, relatifA: .title3))
                                .foregroundColor(choisi == exercice ? Tokens.Couleurs.accent : Tokens.Couleurs.texteSecondaire)
                        }
                    }
                }
            }
            .padding(.top, Tokens.Espacements.e8)
            .entree(rang: 2)
        } bas: {
            BoutonPrincipal(L.t(contexte == .seance ? "exercice.bouton" : "commun.enregistrer"), action: valider)
                .entree(rang: 4)
        }
        .onAppear {
            // Un exercice réservé à l'abonnement ne peut pas rester choisi.
            if contexte == .seance, reserve(etat.exercice) {
                etat.exercice = .pompes
            }
        }
    }

    private func choisir(_ exercice: Exercice) {
        if reserve(exercice) {
            ouvrirPaywall()
            return
        }
        if contexte == .seance {
            etat.exercice = exercice
        } else {
            etat.definirExerciceParDefaut(exercice)
        }
    }
}
