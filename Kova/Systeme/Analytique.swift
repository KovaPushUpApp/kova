import Foundation
import TelemetryDeck

/// Mesure d'audience légère et sans identifiant publicitaire (TelemetryDeck).
/// Seuls les événements du brief (section 10) partent. Aucune image, aucun nom
/// d'app, aucune donnée personnelle.
enum Analytique {
    enum Evenement: String {
        case onboardingEtapeVue = "onboarding_etape_vue"
        case autorisationAccordee = "autorisation_accordee"
        case autorisationRefusee = "autorisation_refusee"
        case essaiReussi = "essai_reussi"
        case paywallVu = "paywall_vu"
        case paywallFerme = "paywall_ferme"
        case offreVue = "offre_vue"
        case achat = "achat"
        case seanceCommencee = "seance_commencee"
        case seanceValidee = "seance_validee"
        case seanceAbandonnee = "seance_abandonnee"
        case fenetreOuverte = "fenetre_ouverte"
        case urgenceUtilisee = "urgence_utilisee"
        case regleModifiee = "regle_modifiee"
    }

    private static var actif = false

    /// Démarre la mesure si un identifiant d'app TelemetryDeck est renseigné
    /// (réglage KOVA_TELEMETRYDECK_ID de project.base.yml). Sinon, rien ne part.
    static func demarrer() {
        guard let identifiant = Bundle.main.object(forInfoDictionaryKey: "KovaTelemetryDeckID") as? String,
              !identifiant.isEmpty else {
            return
        }
        TelemetryDeck.initialize(config: TelemetryDeck.Config(appID: identifiant))
        actif = true
    }

    static func envoyer(_ evenement: Evenement, _ parametres: [String: String] = [:]) {
        guard actif else { return }
        TelemetryDeck.signal(evenement.rawValue, parameters: parametres)
    }
}
