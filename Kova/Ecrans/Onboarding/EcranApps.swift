import SwiftUI

/// Bloc commun aux écrans Apps et AppsReglage : le bouton qui ouvre le
/// sélecteur d'apps, puis la liste des apps choisies.
///
/// Adaptation de la section 9 du brief : pas de liste maison avec interrupteurs,
/// les apps choisies sont des jetons opaques. À l'étape 4, le bouton ouvre le
/// sélecteur système d'Apple et la liste affiche ses Label.
struct ListeApps: View {
    let apps: [String]
    let choisir: () -> Void

    var body: some View {
        VStack(spacing: Tokens.Espacements.e8) {
            BoutonPointille(titre: L.t("apps.choisir"), action: choisir)
            ForEach(apps, id: \.self) { app in
                LigneApp(nom: app)
            }
        }
    }
}

/// Écran 5 : choix des apps à verrouiller.
struct EcranApps: View {
    @EnvironmentObject private var etat: EtatApp
    let retour: () -> Void
    let suivant: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            EnTeteEtapes(etape: 4, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.t("apps.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            Text(verbatim: L.n("apps.compte", etat.apps.count))
                .paragraphe()
                .entree(rang: 2)
            ListeApps(apps: etat.apps, choisir: { etat.apps = DonneesFictives.appsVerrouillees })
                .entree(rang: 3)
        } bas: {
            BoutonPrincipal(L.t("apps.bouton"), action: suivant)
                .entree(rang: 5)
        }
    }
}
