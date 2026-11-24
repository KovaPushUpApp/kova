import Combine
import Foundation

/// État de l'app : l'onboarding, les options, et l'état du jeu (solde, règle,
/// fenêtre, planning, séances). Tout reste sur le téléphone, dans l'App Group.
final class EtatApp: ObservableObject {
    private let reglages: UserDefaults
    /// Calendrier dont la semaine commence le lundi.
    let calendrier = Semaine.calendrier()

    @Published var onboardingTermine: Bool {
        didSet { reglages.set(onboardingTermine, forKey: CleStockage.onboardingTermine) }
    }

    /// L'état du jeu. Il ne se modifie que par les fonctions ci-dessous, qui
    /// l'enregistrent et mettent à jour le verrouillage.
    @Published private(set) var jeu: EtatJeu

    // Réponses de l'onboarding.
    @Published var tempsParJour: TempsParJour {
        didSet { reglages.set(tempsParJour.rawValue, forKey: CleStockage.tempsParJour) }
    }
    @Published var objectifs: Set<Objectif> {
        didSet { reglages.set(objectifs.map(\.rawValue), forKey: CleStockage.objectifs) }
    }

    // Options.
    @Published var compterAVoixHaute: Bool {
        didSet { reglages.set(compterAVoixHaute, forKey: CleStockage.compterAVoixHaute) }
    }
    @Published var rappelQuotidien: Bool {
        didSet {
            reglages.set(rappelQuotidien, forKey: CleStockage.rappelQuotidien)
            programmerRappel()
        }
    }

    /// Accord pour la mesure d'audience anonyme. Non tant qu'on n'a pas dit oui.
    @Published var mesureAcceptee: Bool {
        didSet {
            reglages.set(mesureAcceptee, forKey: CleStockage.mesureAcceptee)
            Analytique.regler(acceptee: mesureAcceptee)
        }
    }
    /// Vrai une fois la question posée : on ne la repose pas.
    var mesureDemandee: Bool {
        get { reglages.bool(forKey: CleStockage.mesureDemandee) }
        set { reglages.set(newValue, forKey: CleStockage.mesureDemandee) }
    }

    /// L'offre de rattrapage ne s'affiche qu'une fois.
    @Published var offreDejaVue: Bool {
        didSet { reglages.set(offreDejaVue, forKey: CleStockage.offreDejaVue) }
    }

    /// Exercice de la séance en cours.
    @Published var exercice: Exercice

    /// Vrai quand l'utilisateur vient de l'écran de verrou d'une app bloquée.
    @Published var seanceDemandee = false

    /// Dernier état "apps ouvertes ou non" envoyé au système, pour ne le
    /// solliciter que quand quelque chose change.
    private var dernierEtatOuvert: Bool?
    /// Une demande venue de l'écran de verrou n'est suivie que pendant ce délai.
    private static let validiteDeLaDemande: TimeInterval = 10 * 60

    init(reglages: UserDefaults = GroupePartage.reglages) {
        self.reglages = reglages
        var etat = DepotJeu.lire(reglages)
        etat.mettreAJour(Date())
        self.jeu = etat
        self.exercice = etat.exerciceParDefaut
        self.onboardingTermine = reglages.bool(forKey: CleStockage.onboardingTermine)
        self.tempsParJour = TempsParJour(rawValue: reglages.string(forKey: CleStockage.tempsParJour) ?? "") ?? .cinqASix
        let choisis = reglages.stringArray(forKey: CleStockage.objectifs) ?? []
        self.objectifs = Set(choisis.compactMap { Objectif(rawValue: $0) })
        self.compterAVoixHaute = reglages.bool(forKey: CleStockage.compterAVoixHaute)
        self.rappelQuotidien = reglages.bool(forKey: CleStockage.rappelQuotidien)
        self.offreDejaVue = reglages.bool(forKey: CleStockage.offreDejaVue)
        self.mesureAcceptee = reglages.bool(forKey: CleStockage.mesureAcceptee)
    }

    var regle: Regle { jeu.regle }

    /// Répétitions demandées pour l'exercice choisi, selon la règle.
    var repetitionsDeLaSeance: Int {
        jeu.regle.repetitionsDemandees(pour: exercice)
    }

    // MARK: Changements d'état

    /// Applique un changement à l'état du jeu, l'enregistre, puis met à jour
    /// les boucliers et les réveils de l'extension.
    @discardableResult
    private func modifier<Resultat>(
        planning: Bool = false,
        forcer: Bool = false,
        _ changement: (inout EtatJeu, Date, Calendar) -> Resultat
    ) -> Resultat {
        var etat = DepotJeu.lire(reglages)
        let maintenant = Date()
        etat.mettreAJour(maintenant)
        let resultat = changement(&etat, maintenant, calendrier)
        let change = etat != jeu
        if change {
            DepotJeu.ecrire(etat, reglages)
            jeu = etat
        }
        // Rien n'est verrouillé avant la fin de l'onboarding. Ensuite, le système
        // n'est sollicité que si l'état, ou le fait que les apps soient ouvertes, a changé.
        guard onboardingTermine else { return resultat }
        let ouvert = etat.appsOuvertes(maintenant, calendrier)
        if change || planning || forcer || ouvert != dernierEtatOuvert {
            dernierEtatOuvert = ouvert
            Verrouillage.synchroniser(etat, maintenant, calendrier, planning: planning)
        }
        return resultat
    }

    /// Recalcule tout à partir de l'heure qu'il est. Appelé à chaque retour
    /// dans l'app et régulièrement tant qu'elle est ouverte : c'est le filet de
    /// sécurité si un réveil de l'extension a manqué.
    func rafraichir(planning: Bool = false) {
        modifier(planning: planning, forcer: planning) { _, _, _ in }
        if reglages.bool(forKey: CleStockage.seanceDemandee) {
            reglages.set(false, forKey: CleStockage.seanceDemandee)
            // Une demande ancienne et ignorée ne doit pas resurgir des jours plus tard.
            let age = Date().timeIntervalSince1970 - reglages.double(forKey: CleStockage.seanceDemandeeLe)
            if age < EtatApp.validiteDeLaDemande {
                seanceDemandee = true
            }
        }
    }

    /// Les apps choisies ont changé : les boucliers doivent suivre.
    func appsModifiees() {
        modifier(forcer: true) { _, _, _ in }
    }

    /// Programme les rappels des prochains jours avec la série en jeu. À refaire
    /// quand l'état change : le texte d'une notification locale est figé d'avance.
    func programmerRappel() {
        Notifications.programmerRappel(actif: rappelQuotidien, jeu: jeu, calendrier: calendrier)
    }

    /// Valide la séance avec les répétitions vraiment faites, au-delà de la série
    /// si on a continué.
    func validerSeance(repetitions: Int) {
        let fait = exercice
        modifier { etat, maintenant, calendrier in
            etat.validerSeance(fait, maintenant, calendrier, repetitions: repetitions)
        }
        Analytique.envoyer(.seanceValidee, [
            "exercice": fait.rawValue,
            "repetitions": String(jeu.seances.last?.repetitions ?? repetitions),
        ])
        if rappelQuotidien {
            // La journée est gagnée : le rappel de ce soir n'a plus lieu d'être.
            programmerRappel()
        }
    }

    /// Ouvre une fenêtre avec tout le solde. Nil si rien n'a pu s'ouvrir.
    @discardableResult
    func ouvrirFenetre() -> Date? {
        let fin = modifier { etat, maintenant, calendrier in etat.ouvrirFenetre(maintenant, calendrier) }
        if fin != nil {
            Analytique.envoyer(.fenetreOuverte)
        }
        return fin
    }

    @discardableResult
    func utiliserUrgence() -> Date? {
        let fin = modifier { etat, maintenant, calendrier in etat.utiliserUrgence(maintenant, calendrier) }
        if fin != nil {
            Analytique.envoyer(.urgenceUtilisee)
        }
        return fin
    }

    /// Change la règle. Avec `immediat`, elle s'applique tout de suite. Sans, assouplir
    /// est différé de 24 h. Renvoie vrai si le changement est différé.
    @discardableResult
    func proposerRegle(_ regle: Regle, immediat: Bool = false) -> Bool {
        let differe = modifier { etat, maintenant, _ -> Bool in
            if immediat {
                // Onboarding et réglages : la règle change tout de suite, dans les deux sens.
                etat.regle = regle
                etat.regleEnAttente = nil
                etat.regleEnAttenteLe = nil
                return false
            }
            return etat.proposerRegle(regle, maintenant)
        }
        Analytique.envoyer(.regleModifiee, ["repetitions": String(regle.repetitions), "minutes": String(regle.minutes)])
        return differe
    }

    /// Active ou coupe le mode hardcore. Couper prend effet dans 24 h, sauf
    /// pendant l'onboarding.
    func definirHardcore(_ actif: Bool, immediat: Bool) {
        modifier { etat, maintenant, calendrier in
            etat.definirHardcore(actif, maintenant, calendrier, immediat: immediat)
        }
    }

    func basculerPlage(_ id: UUID) {
        modifier(planning: true) { etat, maintenant, _ in etat.basculerPlage(id, maintenant) }
    }

    func ajouterPlage(_ plage: Plage) {
        modifier(planning: true) { etat, _, _ in etat.ajouterPlage(plage) }
    }

    func definirUrgenceActivee(_ active: Bool) {
        modifier { etat, _, _ in etat.urgenceActivee = active }
    }

    func definirExerciceParDefaut(_ exercice: Exercice) {
        self.exercice = exercice
        modifier { etat, _, _ in etat.exerciceParDefaut = exercice }
    }
}
