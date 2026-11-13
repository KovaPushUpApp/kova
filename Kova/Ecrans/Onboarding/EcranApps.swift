import SwiftUI

/// Écran 5 : choix des apps à verrouiller.
///
/// Adaptation de la section 9 du brief : le titre et le bouton de la maquette
/// sont gardés, le sélecteur système d'Apple s'ouvre au milieu, et les apps
/// choisies s'affichent avec le composant Label d'Apple. Le temps d'usage par
/// app de la maquette est retiré.
struct EcranApps: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var apps: GestionApps
    let retour: () -> Void
    let suivant: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            EnTeteEtapes(etape: 5, total: EtapeOnboarding.nombreEtapes, retour: retour)
            Text(verbatim: L.t("apps.titre"))
                .titreEcran()
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 1)
            Text(verbatim: apps.nombre == 0 ? L.t("apps.aucune") : L.n("apps.compte", apps.nombre))
                .paragraphe()
                .entree(rang: 2)
            SelecteurApps(apps: apps) { choix in
                // Pendant l'onboarding, rien n'est encore verrouillé : pas de délai.
                apps.enregistrer(choix, immediat: true)
            }
            .entree(rang: 3)
        } bas: {
            BoutonPrincipal(L.t("apps.bouton"), action: suivant)
                .opacity(apps.nombre == 0 ? Double(Tokens.Opacites.scan) : 1)
                .disabled(apps.nombre == 0)
                .entree(rang: 5)
        }
    }
}

/// Affiché quand l'utilisateur n'a pas pris l'abonnement et a choisi plus d'une
/// app : sans abonnement, une seule app peut être verrouillée (brief, section 4).
struct EcranLimiteGratuite: View {
    @EnvironmentObject private var apps: GestionApps
    let voirAbonnement: () -> Void
    let terminer: () -> Void

    var body: some View {
        EcranKova {
            IconeAnneau(icone: .cadenas, avecAnneau: false)
            Text(verbatim: L.t("limite.titre"))
                .titreEcran()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 2)
            Text(verbatim: L.t("limite.texte"))
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 3)
            SelecteurApps(apps: apps) { choix in
                apps.enregistrer(choix, immediat: true)
            }
            .entree(rang: 4)
        } bas: {
            if apps.respecteLaLimiteGratuite && apps.nombre > 0 {
                BoutonPrincipal(L.t("commun.continuer"), action: terminer)
            }
            BoutonSecondaire(L.t("limite.abonnement"), action: voirAbonnement)
        }
    }
}
