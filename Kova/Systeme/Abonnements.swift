import Combine
import Foundation
import RevenueCat

/// Les abonnements, gérés avec RevenueCat (brief, section 4).
///
/// À configurer dans RevenueCat et App Store Connect (action humaine) :
///   - un droit nommé "premium" ;
///   - une offre courante avec un forfait annuel (essai gratuit de 3 jours)
///     et un forfait hebdomadaire ;
///   - une offre nommée "rattrapage" avec un forfait annuel dont la première
///     année est à prix réduit (offre de lancement payée d'avance).
/// Les prix affichés viennent de l'App Store. L'offre courante est celle que
/// RevenueCat renvoie, ce qui permet ses tests de prix sans changer le code.
final class Abonnements: ObservableObject {
    static let droit = "premium"
    static let offreDeRattrapage = "rattrapage"

    @Published private(set) var estAbonne = false
    @Published private(set) var annuel: Package?
    @Published private(set) var hebdomadaire: Package?
    @Published private(set) var rattrapage: Package?
    /// Vrai si l'essai gratuit de l'annuel est encore proposable à cet utilisateur.
    @Published private(set) var essaiDisponible = true
    /// Détail de l'abonnement en cours, s'il y en a un.
    @Published private(set) var abonnement: EntitlementInfo?
    @Published private(set) var enCours = false
    /// Dernier message d'erreur à montrer, déjà traduit.
    @Published var erreur: String?

    /// Faux tant qu'aucune clé RevenueCat n'est renseignée (réglage
    /// KOVA_REVENUECAT_CLE de project.base.yml) : les achats sont alors indisponibles.
    private(set) var configure = false

    /// Réglage de chantier : débloque tout sans achat. Retiré à l'étape 7.
    @Published var simulation = false {
        didSet { estAbonne = simulation || (abonnement?.isActive ?? false) }
    }

    func demarrer() {
        guard !configure,
              let cle = Bundle.main.object(forInfoDictionaryKey: "KovaRevenueCatCle") as? String,
              !cle.isEmpty else {
            return
        }
        Purchases.configure(withAPIKey: cle)
        configure = true
        Task { await charger() }
        Task { [weak self] in
            for await info in Purchases.shared.customerInfoStream {
                await self?.appliquer(info)
            }
        }
    }

    @MainActor
    private func appliquer(_ info: CustomerInfo) {
        abonnement = info.entitlements[Abonnements.droit]
        estAbonne = simulation || (abonnement?.isActive ?? false)
    }

    /// Charge les offres et l'état de l'abonnement.
    @MainActor
    func charger() async {
        guard configure else { return }
        if let offres = try? await Purchases.shared.offerings() {
            annuel = offres.current?.annual
            hebdomadaire = offres.current?.weekly
            let secours = offres.offering(identifier: Abonnements.offreDeRattrapage)
            rattrapage = secours?.annual ?? secours?.availablePackages.first
            if let produit = annuel?.storeProduct {
                let statut = await Purchases.shared.checkTrialOrIntroDiscountEligibility(product: produit)
                essaiDisponible = statut != .ineligible && produit.introductoryDiscount != nil
            }
        }
        if let info = try? await Purchases.shared.customerInfo() {
            appliquer(info)
        }
    }

    /// Lance l'achat d'un forfait. Renvoie vrai si l'abonnement est actif ensuite.
    @MainActor
    func acheter(_ forfait: Package?) async -> Bool {
        guard configure, let forfait else {
            erreur = L.t("achat.indisponible")
            return false
        }
        enCours = true
        defer { enCours = false }
        do {
            let resultat = try await Purchases.shared.purchase(package: forfait)
            if resultat.userCancelled {
                return false
            }
            appliquer(resultat.customerInfo)
            if estAbonne {
                Analytique.envoyer(.achat, ["formule": forfait.storeProduct.productIdentifier])
            }
            return estAbonne
        } catch {
            erreur = L.t("achat.echec")
            return false
        }
    }

    /// Restaure les achats. Renvoie vrai si un abonnement actif a été retrouvé.
    @MainActor
    func restaurer() async -> Bool {
        guard configure else {
            erreur = L.t("achat.indisponible")
            return false
        }
        enCours = true
        defer { enCours = false }
        do {
            appliquer(try await Purchases.shared.restorePurchases())
            if !estAbonne {
                erreur = L.t("achat.rien_a_restaurer")
            }
            return estAbonne
        } catch {
            erreur = L.t("achat.echec")
            return false
        }
    }

    /// Formule de l'abonnement en cours, si elle correspond à un forfait connu.
    var formuleActuelle: Formule? {
        guard let produit = abonnement?.productIdentifier, abonnement?.isActive == true else { return nil }
        if produit == hebdomadaire?.storeProduct.productIdentifier {
            return .hebdomadaire
        }
        return .annuel
    }
}
