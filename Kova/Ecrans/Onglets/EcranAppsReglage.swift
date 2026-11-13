import FamilyControls
import SwiftUI

/// Écran 22 : modifier les apps verrouillées. Même sélecteur que l'onboarding.
/// Ajouter une app est immédiat, retirer une app prend effet 24 h plus tard
/// (brief, règle 7). Sans abonnement, une seule app.
struct EcranAppsReglage: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var apps: GestionApps
    @EnvironmentObject private var abonnements: Abonnements
    let retour: () -> Void
    let ouvrirPaywall: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            HStack(spacing: Tokens.Espacements.e13) {
                BoutonRetour(action: retour)
                Text(verbatim: L.t("ecran.apps_reglage")).titreCarte()
            }
            Text(verbatim: L.n("apps_reglage.texte", apps.nombre))
                .paragraphe()
                .entree(rang: 1)
            SelecteurApps(apps: apps, surChoix: choisir)
                .entree(rang: 3)
        } bas: {
            BoutonPrincipal(L.t("commun.enregistrer"), action: retour)
                .entree(rang: 4)
        }
    }

    private func choisir(_ choix: FamilyActivitySelection) {
        if !abonnements.estAbonne, !GestionApps.respecteLaLimiteGratuite(choix) {
            // Plus d'une app demande l'abonnement : le choix n'est pas enregistré.
            ouvrirPaywall()
            return
        }
        apps.enregistrer(choix, immediat: false)
        etat.appsModifiees()
    }
}
