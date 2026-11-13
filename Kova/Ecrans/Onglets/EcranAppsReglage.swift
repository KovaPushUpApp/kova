import SwiftUI

/// Écran 22 : modifier les apps verrouillées. Même sélection que l'onboarding.
/// Retirer une app prend effet 24 h plus tard (logique à l'étape 5).
struct EcranAppsReglage: View {
    @EnvironmentObject private var etat: EtatApp
    let retour: () -> Void
    let enregistrer: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(spacing: Tokens.Espacements.e13) {
                BoutonRetour(action: retour)
                Text(verbatim: L.t("ecran.apps_reglage")).titreCarte()
            }
            Text(verbatim: L.n("apps_reglage.texte", etat.apps.count))
                .paragraphe()
                .entree(rang: 1)
            ListeApps(apps: etat.apps, choisir: { etat.apps = DonneesFictives.appsVerrouillees })
                .entree(rang: 3)
        } bas: {
            BoutonPrincipal(L.t("commun.enregistrer"), action: enregistrer)
                .entree(rang: 4)
        }
    }
}
