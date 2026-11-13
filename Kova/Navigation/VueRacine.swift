import AVFoundation
import SwiftUI

/// Aiguillage : onboarding tant qu'il n'est pas terminé, puis l'app.
struct VueRacine: View {
    @EnvironmentObject private var etat: EtatApp

    var body: some View {
        ZStack {
            Tokens.Couleurs.fond.ignoresSafeArea()
            if etat.onboardingTermine {
                VuePrincipale()
            } else {
                ParcoursOnboarding()
            }
        }
    }
}

/// Les écrans de l'onboarding, enchaînés dans une pile de navigation.
struct ParcoursOnboarding: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var apps: GestionApps
    @EnvironmentObject private var abonnements: Abonnements
    @State private var chemin: [EtapeOnboarding] = []
    @State private var ecranRefuse = false
    @State private var cameraRefusee = false
    @State private var demandeEnCours = false

    var body: some View {
        NavigationStack(path: $chemin) {
            ecran(.bienvenue)
                .navigationDestination(for: EtapeOnboarding.self) { etape in
                    ecran(etape)
                        .onAppear {
                            Analytique.envoyer(.onboardingEtapeVue, ["etape": String(etape.numero)])
                        }
                }
        }
    }

    @ViewBuilder
    private func ecran(_ etape: EtapeOnboarding) -> some View {
        switch etape {
        case .bienvenue:
            EcranBienvenue(commencer: { aller(.temps) }, restaurer: restaurer)
                .alerteAchat(abonnements)
        case .temps:
            EcranTemps(retour: reculer, suivant: { aller(.choc) })
        case .choc:
            EcranChoc(retour: reculer, suivant: { aller(.objectif) })
        case .objectif:
            EcranObjectif(retour: reculer, suivant: { aller(.autoEcran) })
        case .autoEcran:
            EcranAutorisation.tempsDEcran(
                refuse: ecranRefuse,
                enCours: demandeEnCours,
                retour: reculer,
                autoriser: demanderTempsDEcran
            )
        case .apps:
            EcranApps(retour: reculer, suivant: { aller(.regle) })
        case .regle:
            EcranRegle(contexte: .onboarding, retour: reculer, valider: { aller(.autoCamera) })
        case .autoCamera:
            EcranAutorisation.camera(refuse: cameraRefusee, retour: reculer, autoriser: demanderCamera)
        case .essai:
            EcranEssai(retour: reculer, suivant: { aller(.paywall) })
        case .paywall:
            EcranPaywall(fermer: fermerPaywall, abonne: terminer)
        case .offre:
            EcranOffre(fermer: apresRefus, abonne: terminer)
        case .limite:
            EcranLimiteGratuite(voirAbonnement: { aller(.paywall) }, terminer: terminer)
        }
    }

    private func aller(_ etape: EtapeOnboarding) {
        // Un double appui ne doit pas empiler deux fois le même écran.
        if chemin.last != etape {
            chemin.append(etape)
        }
    }

    private func reculer() {
        if !chemin.isEmpty {
            chemin.removeLast()
        }
    }

    // MARK: Autorisations

    /// On ne peut pas continuer sans l'accès Temps d'écran (brief, section 7).
    private func demanderTempsDEcran() {
        demandeEnCours = true
        Task {
            let accorde = await apps.demanderAutorisation()
            ecranRefuse = !accorde
            Analytique.envoyer(accorde ? .autorisationAccordee : .autorisationRefusee, ["type": "ecran"])
            if accorde {
                // L'écran de verrou a besoin des notifications pour proposer d'ouvrir Kova.
                Notifications.demanderAutorisation { _ in
                    demandeEnCours = false
                    aller(.apps)
                }
            } else {
                demandeEnCours = false
            }
        }
    }

    private func demanderCamera() {
        guard CameraSeance.disponible else {
            aller(.essai)
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            aller(.essai)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { accorde in
                DispatchQueue.main.async {
                    cameraRefusee = !accorde
                    Analytique.envoyer(accorde ? .autorisationAccordee : .autorisationRefusee, ["type": "camera"])
                    if accorde {
                        aller(.essai)
                    }
                }
            }
        default:
            // Déjà refusée : seul un passage par les Réglages peut changer ça.
            cameraRefusee = true
        }
    }

    // MARK: Fin du parcours

    private func restaurer() {
        Task {
            if await abonnements.restaurer() {
                aller(.temps)
            }
        }
    }

    /// Fermer le paywall sans payer montre l'offre de rattrapage, une seule fois.
    private func fermerPaywall() {
        if etat.offreDejaVue {
            apresRefus()
        } else {
            etat.offreDejaVue = true
            aller(.offre)
        }
    }

    /// Sans abonnement, une seule app peut rester verrouillée.
    private func apresRefus() {
        if apps.respecteLaLimiteGratuite {
            terminer()
        } else {
            aller(.limite)
        }
    }

    private func terminer() {
        if !abonnements.estAbonne && !apps.respecteLaLimiteGratuite {
            aller(.limite)
            return
        }
        etat.onboardingTermine = true
        // Premier verrouillage : les apps choisies sont bloquées dès maintenant.
        etat.rafraichir(planning: true)
    }
}
