import SwiftUI

/// L'app après l'onboarding : quatre onglets, et les écrans poussés par dessus.
struct VuePrincipale: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var abonnements: Abonnements
    @EnvironmentObject private var apps: GestionApps
    @Environment(\.scenePhase) private var phase

    @State private var onglet: Onglet = .accueil
    @State private var chemin: [Destination] = []
    @State private var paywallOuvert = false
    /// La caméra de la séance, partagée entre le placement et le compteur.
    @StateObject private var camera = CameraSeance()

    /// Recalcul régulier tant que l'app est ouverte : filet de sécurité du reverrouillage.
    private let battement = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    var body: some View {
        NavigationStack(path: $chemin) {
            VStack(spacing: 0) {
                contenuOnglet
                BarreOnglets(selection: $onglet)
                    .padding(.horizontal, Tokens.Ecran.margeCotes)
                    .padding(.bottom, Tokens.Espacements.e8)
            }
            .background(Tokens.Couleurs.fond.ignoresSafeArea())
            .navigationDestination(for: Destination.self) { destination in
                ecran(destination)
            }
        }
        .fullScreenCover(isPresented: $paywallOuvert) {
            EcranPaywall(fermer: { paywallOuvert = false }, abonne: { paywallOuvert = false })
                .preferredColorScheme(.dark)
        }
        .onAppear {
            etat.rafraichir(planning: true)
            etat.programmerRappel()
            apps.relireSursis()
            ouvrirLaSeanceDemandee()
        }
        .onChange(of: phase) { nouvelle in
            if nouvelle == .active {
                etat.rafraichir()
                // Les rappels à venir suivent la série, les apps en sursis leur échéance.
                etat.programmerRappel()
                apps.relireSursis()
                Task { await abonnements.charger() }
            }
        }
        .onReceive(battement) { _ in
            if phase == .active {
                etat.rafraichir()
            }
        }
        .onChange(of: chemin) { nouveau in
            // La caméra s'éteint dès qu'on quitte le placement et le compteur.
            if !nouveau.contains(.placement) && !nouveau.contains(.compteur) {
                camera.arreter()
            }
        }
        .onChange(of: etat.seanceDemandee) { _ in
            ouvrirLaSeanceDemandee()
        }
    }

    /// L'utilisateur arrive de l'écran de verrou d'une app bloquée.
    private func ouvrirLaSeanceDemandee() {
        guard etat.seanceDemandee else { return }
        etat.seanceDemandee = false
        paywallOuvert = false
        onglet = .accueil
        chemin = [.verrou]
    }

    @ViewBuilder
    private var contenuOnglet: some View {
        switch onglet {
        case .accueil:
            EcranAccueil(gagnerDuTemps: { aller(.exercice) }, ouvrirVerrou: { aller(.verrou) })
        case .stats:
            if abonnements.estAbonne {
                EcranStats()
            } else {
                EcranReserve(titre: L.t("stats.titre"), texte: L.t("reserve.stats"), voir: { paywallOuvert = true })
            }
        case .planning:
            EcranPlanning(ouvrirPaywall: { paywallOuvert = true })
        case .reglages:
            EcranReglages(ouvrir: { aller($0) })
        }
    }

    @ViewBuilder
    private func ecran(_ destination: Destination) -> some View {
        switch destination {
        case .verrou:
            EcranVerrou(
                commencer: { chemin = [.exercice] },
                fermer: { reculer() },
                urgence: { aller(.urgence) }
            )
        case .exercice:
            EcranExercice(
                contexte: .seance,
                retour: { reculer() },
                valider: { aller(.placement) },
                ouvrirPaywall: { paywallOuvert = true }
            )
        case .placement:
            EcranPlacement(camera: camera, retour: { reculer() }, suivant: { aller(.compteur) })
        case .compteur:
            EcranCompteur(
                camera: camera,
                abandonner: { revenirAuxOnglets() },
                valider: { repetitions in
                    // Les minutes vont au solde dès la validation (brief, règle 3).
                    etat.validerSeance(repetitions: repetitions)
                    aller(.gagne)
                }
            )
        case .gagne:
            EcranGagne(
                ouvrir: {
                    etat.ouvrirFenetre()
                    revenirAuxOnglets()
                },
                garder: { revenirAuxOnglets() }
            )
        case .urgence:
            EcranUrgence(
                retour: { reculer() },
                debloquer: {
                    etat.utiliserUrgence()
                    revenirAuxOnglets()
                },
                faireSesPompes: { chemin = [.exercice] }
            )
        case .appsReglage:
            EcranAppsReglage(retour: { reculer() }, ouvrirPaywall: { paywallOuvert = true })
        case .regle:
            EcranRegle(contexte: .reglages, retour: { reculer() }, valider: { reculer() })
        case .exerciceParDefaut:
            EcranExercice(
                contexte: .parDefaut,
                retour: { reculer() },
                valider: { reculer() },
                ouvrirPaywall: { paywallOuvert = true }
            )
        case .abonnement:
            EcranAbonnement(retour: { reculer() }, ouvrirPaywall: { paywallOuvert = true })
        case .controleSocle:
            ControleSocle(retour: { reculer() })
        }
    }

    private func aller(_ destination: Destination) {
        chemin.append(destination)
    }

    private func reculer() {
        if !chemin.isEmpty {
            chemin.removeLast()
        }
    }

    private func revenirAuxOnglets() {
        chemin.removeAll()
    }
}

/// Onglet réservé à l'abonnement : un titre, une phrase et l'accès au paywall.
struct EcranReserve: View {
    let titre: String
    let texte: String
    let voir: () -> Void

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            Text(verbatim: titre).titreEcran()
            IconeAnneau(icone: .cadenas, avecAnneau: false)
            Text(verbatim: texte)
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 2)
            BoutonPrincipal(L.t("abonnement.voir"), action: voir)
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 3)
        }
    }
}

/// Barre d'onglets de la maquette : carte arrondie, onglet actif en fond accent.
struct BarreOnglets: View {
    @Binding var selection: Onglet

    var body: some View {
        HStack(spacing: Tokens.Espacements.e3) {
            ForEach(Onglet.allCases, id: \.self) { onglet in
                bouton(onglet)
            }
        }
        .padding(Tokens.Espacements.e8)
        .background(Tokens.Couleurs.carte)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
    }

    private func bouton(_ onglet: Onglet) -> some View {
        let actif = onglet == selection
        let couleur = actif ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texteSecondaire
        return Button {
            selection = onglet
        } label: {
            VStack(spacing: Tokens.Espacements.e3) {
                Icone(nom: onglet.icone, couleur: couleur)
                Text(verbatim: onglet.titre)
                    .legende(couleur, graisse: .demiGras)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .frame(height: Tokens.Hauteurs.onglet)
            .background(actif ? Tokens.Couleurs.accent : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(actif ? .isSelected : [])
    }
}
