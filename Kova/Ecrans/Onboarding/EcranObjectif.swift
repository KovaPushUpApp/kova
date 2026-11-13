import SwiftUI

/// Écran 4 : "Tu veux quoi, au fond ?" Plusieurs choix possibles.
struct EcranObjectif: View {
    @EnvironmentObject private var etat: EtatApp
    let retour: () -> Void
    let suivant: () -> Void

    var body: some View {
        EcranKova {
            EnTeteEtapes(etape: 3, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.t("objectif.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            Text(verbatim: L.t("objectif.texte"))
                .paragraphe()
                .entree(rang: 2)
            VStack(spacing: Tokens.Espacements.e13) {
                ForEach(Objectif.allCases, id: \.self) { objectif in
                    CarteOption(choisie: etat.objectifs.contains(objectif), action: { basculer(objectif) }) {
                        HStack {
                            Text(verbatim: objectif.libelle).corps(graisse: .demiGras)
                            Spacer(minLength: Tokens.Espacements.e8)
                            if etat.objectifs.contains(objectif) {
                                Text(verbatim: L.t("objectif.choisi")).legende(graisse: .moyen)
                            }
                        }
                    }
                }
            }
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 3)
        } bas: {
            BoutonPrincipal(L.t("commun.continuer"), action: suivant)
                .entree(rang: 5)
        }
    }

    private func basculer(_ objectif: Objectif) {
        if etat.objectifs.contains(objectif) {
            etat.objectifs.remove(objectif)
        } else {
            etat.objectifs.insert(objectif)
        }
    }
}
