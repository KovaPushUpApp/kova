import DeviceActivity
import FamilyControls
import Foundation
import ManagedSettings

/// Le verrouillage réel des apps, avec l'API Temps d'écran d'Apple.
/// Partagé entre l'app et l'extension de surveillance, qui reverrouille
/// quand Kova est fermée.
///
/// Principe : l'état du jeu (solde, fenêtre, planning) fait foi. À chaque
/// événement, on relit cet état et on pose ou on retire les boucliers. Aucun
/// rappel du système n'est supposé fiable à lui seul : l'app refait le même
/// calcul à chaque ouverture.
enum Verrouillage {
    /// Faux dans le build du socle, compilé sans la capacité Family Controls.
    static var disponible: Bool {
        #if KOVA_SANS_VERROUILLAGE
        return false
        #else
        return true
        #endif
    }

    /// Durée minimale d'un intervalle de surveillance imposée par le système
    /// (15 minutes d'après son message d'erreur, à confirmer sur un iPhone).
    static let intervalleMinimum: TimeInterval = 15 * 60
    /// Marge appliquée par l'extension : un rappel peut arriver quelques
    /// secondes avant l'heure, et les apps doivent quand même se reverrouiller.
    static let margeDeRappel: TimeInterval = 30

    private static let nomFenetre = "fenetre"
    private static let nomUrgence = "urgence"
    private static let nomRetrait = "retrait"
    private static let prefixePlage = "plage-"

    // MARK: Apps choisies

    static func lireSelection(_ cle: String = CleStockage.selection) -> FamilyActivitySelection {
        guard let donnees = GroupePartage.reglages.data(forKey: cle),
              let selection = try? PropertyListDecoder().decode(FamilyActivitySelection.self, from: donnees) else {
            return FamilyActivitySelection()
        }
        return selection
    }

    static func ecrireSelection(_ selection: FamilyActivitySelection, _ cle: String = CleStockage.selection) {
        guard let donnees = try? PropertyListEncoder().encode(selection) else { return }
        GroupePartage.reglages.set(donnees, forKey: cle)
    }

    /// Les apps retirées de la liste restent verrouillées 24 h (brief, règle 7).
    static func selectionRetiree(_ maintenant: Date) -> FamilyActivitySelection {
        let limite = GroupePartage.reglages.double(forKey: CleStockage.selectionRetireeJusquA)
        guard limite > maintenant.timeIntervalSince1970 else {
            return FamilyActivitySelection()
        }
        return lireSelection(CleStockage.selectionRetiree)
    }

    // MARK: Boucliers

    /// Pose ou retire les boucliers selon l'état du jeu à l'instant donné.
    static func appliquer(_ etat: EtatJeu, _ maintenant: Date, _ calendrier: Calendar) {
        guard disponible else { return }
        let magasin = ManagedSettingsStore()
        if etat.appsOuvertes(maintenant, calendrier) {
            magasin.shield.applications = nil
            magasin.shield.applicationCategories = nil
            magasin.shield.webDomains = nil
            magasin.shield.webDomainCategories = nil
            return
        }
        let choisie = lireSelection()
        let retiree = selectionRetiree(maintenant)
        let apps = choisie.applicationTokens.union(retiree.applicationTokens)
        let categories = choisie.categoryTokens.union(retiree.categoryTokens)
        let domaines = choisie.webDomainTokens.union(retiree.webDomainTokens)
        magasin.shield.applications = apps.isEmpty ? nil : apps
        magasin.shield.applicationCategories = categories.isEmpty ? nil : .specific(categories, except: Set())
        magasin.shield.webDomains = domaines.isEmpty ? nil : domaines
        magasin.shield.webDomainCategories = categories.isEmpty ? nil : .specific(categories, except: Set())
    }

    /// Relit l'état enregistré et applique les boucliers. Utilisé par l'extension.
    static func appliquerDepuisLeDepot(marge: TimeInterval = 0) {
        var etat = DepotJeu.lire()
        let instant = Date().addingTimeInterval(marge)
        etat.mettreAJour(instant)
        appliquer(etat, instant, Semaine.calendrier())
    }

    // MARK: Surveillance

    /// Demande au système de réveiller l'extension à une heure donnée, tous les jours.
    ///
    /// Le système ne propose que des intervalles, d'au moins 15 minutes, et
    /// réveille l'extension au début et à la fin. On pose donc un intervalle de
    /// 15 minutes dont un bord tombe sur l'heure voulue : il commence à cette
    /// heure, ou il s'y termine quand on est trop près de minuit, pour ne jamais
    /// chevaucher deux jours. L'extension recalcule l'état à chaque réveil, donc
    /// le réveil en trop à l'autre bord est sans effet.
    private static func poserReveil(_ nom: String, heure: Int, minute: Int, seconde: Int) {
        let instant = heure * 3600 + minute * 60 + seconde
        let duree = Int(intervalleMinimum)
        let finDeJournee = 24 * 3600 - 1
        let debut = instant + duree <= finDeJournee ? instant : instant - duree
        let fin = debut + duree
        let programme = DeviceActivitySchedule(
            intervalStart: DateComponents(hour: debut / 3600, minute: debut % 3600 / 60, second: debut % 60),
            intervalEnd: DateComponents(hour: fin / 3600, minute: fin % 3600 / 60, second: fin % 60),
            repeats: true
        )
        try? DeviceActivityCenter().startMonitoring(DeviceActivityName(nom), during: programme)
    }

    /// Programme le reverrouillage à la fin d'une fenêtre ou d'une urgence.
    private static func programmerFin(_ fin: Date?, nom: String, _ maintenant: Date) {
        DeviceActivityCenter().stopMonitoring([DeviceActivityName(nom)])
        guard let fin, fin > maintenant else { return }
        let composants = Calendar.current.dateComponents([.hour, .minute, .second], from: fin)
        poserReveil(nom, heure: composants.hour ?? 0, minute: composants.minute ?? 0, seconde: composants.second ?? 0)
    }

    /// Programme un réveil au début et un à la fin de chaque plage stricte.
    /// Le jour de la semaine est vérifié au réveil, à partir de l'état du jeu.
    private static func programmerPlanning(_ plages: [Plage], _ maintenant: Date) {
        let centre = DeviceActivityCenter()
        let anciennes = centre.activities.filter { $0.rawValue.hasPrefix(prefixePlage) }
        if !anciennes.isEmpty {
            centre.stopMonitoring(anciennes)
        }
        for plage in plages where plage.enVigueur(maintenant) {
            let nom = prefixePlage + plage.id.uuidString
            poserReveil(nom + "-debut", heure: plage.debut / 60, minute: plage.debut % 60, seconde: 0)
            poserReveil(nom + "-fin", heure: plage.fin / 60, minute: plage.fin % 60, seconde: 0)
        }
    }

    /// À appeler par l'app après chaque changement d'état : boucliers et surveillances.
    static func synchroniser(_ etat: EtatJeu, _ maintenant: Date, _ calendrier: Calendar, planning: Bool) {
        guard disponible else { return }
        appliquer(etat, maintenant, calendrier)
        programmerFin(etat.fenetreFin, nom: nomFenetre, maintenant)
        programmerFin(etat.urgenceFin, nom: nomUrgence, maintenant)
        // Fin du délai de 24 h des apps retirées de la liste.
        let finRetrait = GroupePartage.reglages.double(forKey: CleStockage.selectionRetireeJusquA)
        programmerFin(finRetrait > 0 ? Date(timeIntervalSince1970: finRetrait) : nil, nom: nomRetrait, maintenant)
        if planning {
            programmerPlanning(etat.plages, maintenant)
        }
    }
}
