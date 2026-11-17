import Combine
import FamilyControls
import Foundation
import ManagedSettings

/// Les apps choisies par l'utilisateur et l'autorisation Temps d'écran.
///
/// Les apps sont des jetons opaques fournis par Apple : Kova ne connaît ni
/// leur nom ni leur identifiant, et ne peut les afficher qu'avec le composant
/// Label du système (brief, section 9, contrainte 2).
final class GestionApps: ObservableObject {
    @Published private(set) var selection: FamilyActivitySelection
    @Published private(set) var autorise = false
    /// Les apps retirées de la liste depuis moins de 24 h : encore verrouillées.
    @Published private(set) var retiree = FamilyActivitySelection()
    /// Fin du sursis des apps retirées.
    @Published private(set) var retireeJusquA: Date?
    /// Noms fictifs, utilisés seulement dans le build sans verrouillage.
    @Published private(set) var fictives: [String] = []

    init() {
        selection = Verrouillage.disponible ? Verrouillage.lireSelection() : FamilyActivitySelection()
        if !Verrouillage.disponible {
            fictives = ["Instagram", "TikTok", "YouTube"]
            autorise = true
        }
        relireSursis()
    }

    /// Relit les apps en sursis. Passé le délai de 24 h, il n'y en a plus.
    func relireSursis(_ maintenant: Date = Date()) {
        guard Verrouillage.disponible else { return }
        let limite = GroupePartage.reglages.double(forKey: CleStockage.selectionRetireeJusquA)
        let enSursis = limite > maintenant.timeIntervalSince1970
        let nouvelle = enSursis ? Verrouillage.selectionRetiree(maintenant) : FamilyActivitySelection()
        let jusquA: Date? = enSursis ? Date(timeIntervalSince1970: limite) : nil
        // Rien n'est publié si rien n'a changé, pour ne pas redessiner les écrans.
        if nouvelle != retiree {
            retiree = nouvelle
        }
        if jusquA != retireeJusquA {
            retireeJusquA = jusquA
        }
    }

    /// "Verrouillée jusqu'au…", pour les apps retirées depuis moins de 24 h.
    var noteDeSursis: String? {
        guard Verrouillage.disponible, let limite = retireeJusquA, limite > Date() else { return nil }
        return L.f("apps_reglage.sursis", limite.formatted(date: .abbreviated, time: .shortened))
    }

    /// Nombre d'éléments verrouillés : apps, catégories et sites.
    var nombre: Int {
        guard Verrouillage.disponible else { return fictives.count }
        return selection.applicationTokens.count + selection.categoryTokens.count + selection.webDomainTokens.count
    }

    /// Sans abonnement : une seule app, pas de catégorie ni de site (brief, section 4).
    /// Une catégorie contient un nombre d'apps que Kova ne peut pas connaître.
    static func respecteLaLimiteGratuite(_ selection: FamilyActivitySelection) -> Bool {
        selection.applicationTokens.count <= 1 && selection.categoryTokens.isEmpty && selection.webDomainTokens.isEmpty
    }

    var respecteLaLimiteGratuite: Bool {
        guard Verrouillage.disponible else { return true }
        return GestionApps.respecteLaLimiteGratuite(selection)
    }

    /// Demande l'accès Temps d'écran pour soi-même. Le système affiche sa propre
    /// fenêtre, avec Face ID ou le code. Renvoie vrai si l'accès est accordé.
    @MainActor
    func demanderAutorisation() async -> Bool {
        guard Verrouillage.disponible else {
            autorise = true
            return true
        }
        do {
            try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
            autorise = true
        } catch {
            autorise = false
        }
        return autorise
    }

    /// Enregistre les apps choisies.
    ///
    /// Ajouter une app prend effet tout de suite. Retirer une app prend effet
    /// 24 h plus tard (brief, règle 7) : elle reste verrouillée jusque-là.
    /// `immediat` sert à l'onboarding, avant le premier verrouillage.
    func enregistrer(_ nouvelle: FamilyActivitySelection, immediat: Bool) {
        if !immediat {
            let maintenant = Date()
            var retiree = Verrouillage.selectionRetiree(maintenant)
            let apps = selection.applicationTokens.subtracting(nouvelle.applicationTokens)
            let categories = selection.categoryTokens.subtracting(nouvelle.categoryTokens)
            let domaines = selection.webDomainTokens.subtracting(nouvelle.webDomainTokens)
            if !apps.isEmpty || !categories.isEmpty || !domaines.isEmpty {
                retiree.applicationTokens.formUnion(apps)
                retiree.categoryTokens.formUnion(categories)
                retiree.webDomainTokens.formUnion(domaines)
                Verrouillage.ecrireSelection(retiree, CleStockage.selectionRetiree)
                GroupePartage.reglages.set(
                    maintenant.addingTimeInterval(EtatJeu.delaiAssouplissement).timeIntervalSince1970,
                    forKey: CleStockage.selectionRetireeJusquA
                )
            }
        }
        Verrouillage.ecrireSelection(nouvelle)
        selection = nouvelle
        relireSursis()
    }
}
