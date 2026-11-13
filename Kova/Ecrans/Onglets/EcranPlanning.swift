import SwiftUI

/// Écran 20 : les plages strictes. Pendant une plage active, les apps restent
/// verrouillées même avec du solde. À l'étape 2, les plages sont fictives.
struct EcranPlanning: View {
    @State private var plages = DonneesFictives.plages
    let ajouter: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            Text(verbatim: L.t("planning.titre")).titreEcran()
            Text(verbatim: L.t("planning.texte"))
                .paragraphe()
                .entree(rang: 1)
            ForEach(Array(plages.enumerated()), id: \.element.id) { rang, plage in
                HStack(spacing: Tokens.Espacements.e13) {
                    VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
                        Text(verbatim: plage.nom).corps(graisse: .demiGras)
                        Text(verbatim: plage.horaires).titreCarte()
                        Text(verbatim: plage.jours).legende()
                    }
                    Spacer(minLength: Tokens.Espacements.e8)
                    Interrupteur(
                        actif: plage.active,
                        libelle: L.f("a11y.plage", plage.nom),
                        action: { plages[rang].active.toggle() }
                    )
                }
                .carte()
                .entree(rang: 2 + rang)
            }
            BoutonPointille(titre: L.t("planning.ajouter"), action: ajouter)
                .entree(rang: 6)
        }
    }
}
