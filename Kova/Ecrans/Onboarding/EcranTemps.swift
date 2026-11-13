import SwiftUI

/// Écran 2 : "Combien de temps par jour sur ton téléphone ?" Un seul choix.
struct EcranTemps: View {
    @EnvironmentObject private var etat: EtatApp
    let retour: () -> Void
    let suivant: () -> Void

    var body: some View {
        EcranKova {
            EnTeteEtapes(etape: 1, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.t("temps.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            Text(verbatim: L.t("temps.texte"))
                .paragraphe()
                .entree(rang: 2)
            VStack(spacing: Tokens.Espacements.e13) {
                ForEach(TempsParJour.allCases, id: \.self) { choix in
                    CarteOption(choisie: etat.tempsParJour == choix, action: { etat.tempsParJour = choix }) {
                        HStack {
                            Text(verbatim: choix.libelle).corps(graisse: .demiGras)
                            Spacer(minLength: Tokens.Espacements.e8)
                            if choix == .cinqASix {
                                Text(verbatim: L.t("temps.moyenne")).legende(graisse: .moyen)
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
}
