import Foundation

// Les règles du jeu (brief, section 6), sans interface ni système : chaque
// fonction reçoit l'heure et le calendrier, pour pouvoir être testée.

enum Semaine {
    /// Calendrier dont la semaine commence le lundi.
    static func calendrier(fuseau: TimeZone = .current) -> Calendar {
        var calendrier = Calendar(identifier: .gregorian)
        calendrier.firstWeekday = 2
        calendrier.timeZone = fuseau
        return calendrier
    }

    /// Jour de la semaine : 1 = lundi, 7 = dimanche.
    static func jour(_ date: Date, _ calendrier: Calendar) -> Int {
        let systeme = calendrier.component(.weekday, from: date)
        return (systeme + 5) % 7 + 1
    }

    /// Lundi 0 h de la semaine qui contient `date`.
    static func debut(_ date: Date, _ calendrier: Calendar) -> Date {
        let jourCourant = calendrier.startOfDay(for: date)
        let recul = jour(date, calendrier) - 1
        return calendrier.date(byAdding: .day, value: -recul, to: jourCourant) ?? jourCourant
    }
}

extension Plage {
    /// Vrai si la plage s'applique encore : active, ou désactivée depuis moins de 24 h.
    func enVigueur(_ maintenant: Date) -> Bool {
        if active {
            return true
        }
        if let limite = enVigueurJusquA {
            return maintenant < limite
        }
        return false
    }

    /// Vrai si l'heure donnée tombe dans la plage.
    func contient(_ maintenant: Date, _ calendrier: Calendar) -> Bool {
        let composants = calendrier.dateComponents([.hour, .minute], from: maintenant)
        let minute = (composants.hour ?? 0) * 60 + (composants.minute ?? 0)
        let jour = Semaine.jour(maintenant, calendrier)
        if debut < fin {
            return jours.contains(jour) && minute >= debut && minute < fin
        }
        // La plage passe minuit : elle a commencé aujourd'hui ou hier.
        let veille = jour == 1 ? 7 : jour - 1
        return (jours.contains(jour) && minute >= debut) || (jours.contains(veille) && minute < fin)
    }
}

extension EtatJeu {
    // MARK: Règle

    /// Applique la règle en attente si son délai est passé. À appeler avant de lire l'état.
    mutating func mettreAJour(_ maintenant: Date) {
        if let attente = regleEnAttente, let le = regleEnAttenteLe, maintenant >= le {
            regle = attente
            regleEnAttente = nil
            regleEnAttenteLe = nil
        }
        if let fin = fenetreFin, fin <= maintenant {
            fenetreFin = nil
        }
        if let fin = urgenceFin, fin <= maintenant {
            urgenceFin = nil
        }
        for rang in plages.indices {
            if let limite = plages[rang].enVigueurJusquA, limite <= maintenant {
                plages[rang].enVigueurJusquA = nil
            }
        }
    }

    /// Change la règle. Durcir prend effet tout de suite, assouplir dans 24 h (règle 7).
    /// Renvoie vrai si le changement est différé.
    @discardableResult
    mutating func proposerRegle(_ nouvelle: Regle, _ maintenant: Date) -> Bool {
        if nouvelle == regle {
            regleEnAttente = nil
            regleEnAttenteLe = nil
            return false
        }
        if nouvelle.minutesParPompe > regle.minutesParPompe {
            regleEnAttente = nouvelle
            regleEnAttenteLe = maintenant.addingTimeInterval(EtatJeu.delaiAssouplissement)
            return true
        }
        regle = nouvelle
        regleEnAttente = nil
        regleEnAttenteLe = nil
        return false
    }

    // MARK: Séances et solde

    /// Une séance validée ajoute les minutes de la règle au solde (règles 3 et 9).
    mutating func validerSeance(_ exercice: Exercice, _ maintenant: Date) {
        let seance = Seance(
            date: maintenant,
            exercice: exercice,
            repetitions: regle.repetitionsDemandees(pour: exercice),
            minutes: regle.minutes
        )
        seances.append(seance)
        if seances.count > EtatJeu.historiqueMaximum {
            seances.removeFirst(seances.count - EtatJeu.historiqueMaximum)
        }
        soldeMinutes += seance.minutes
    }

    // MARK: Planning

    func plageActive(_ maintenant: Date, _ calendrier: Calendar) -> Bool {
        plages.contains { $0.enVigueur(maintenant) && $0.contient(maintenant, calendrier) }
    }

    /// Activer est immédiat, désactiver prend effet dans 24 h (règle 7).
    mutating func basculerPlage(_ id: UUID, _ maintenant: Date) {
        guard let rang = plages.firstIndex(where: { $0.id == id }) else { return }
        if plages[rang].active {
            plages[rang].active = false
            plages[rang].enVigueurJusquA = maintenant.addingTimeInterval(EtatJeu.delaiAssouplissement)
        } else {
            plages[rang].active = true
            plages[rang].enVigueurJusquA = nil
        }
    }

    /// Ajouter une plage durcit le jeu : effet immédiat.
    mutating func ajouterPlage(_ plage: Plage) {
        guard plages.count < Plage.nombreMaximum else { return }
        plages.append(plage)
    }

    // MARK: Fenêtre de déblocage

    func fenetreOuverte(_ maintenant: Date) -> Bool {
        if let fin = fenetreFin {
            return fin > maintenant
        }
        return false
    }

    func urgenceEnCours(_ maintenant: Date) -> Bool {
        if let fin = urgenceFin {
            return fin > maintenant
        }
        return false
    }

    /// Vrai si les apps verrouillées sont accessibles maintenant (règles 4, 5 et 6).
    func appsOuvertes(_ maintenant: Date, _ calendrier: Calendar) -> Bool {
        if urgenceEnCours(maintenant) {
            return true
        }
        return fenetreOuverte(maintenant) && !plageActive(maintenant, calendrier)
    }

    /// Début de la prochaine plage stricte après l'instant donné, dans les 8 jours.
    func prochainDebutDePlage(_ maintenant: Date, _ calendrier: Calendar) -> Date? {
        let aujourdHui = calendrier.startOfDay(for: maintenant)
        var prochain: Date?
        for plage in plages where plage.enVigueur(maintenant) {
            for decalage in 0...7 {
                guard let jour = calendrier.date(byAdding: .day, value: decalage, to: aujourdHui),
                      plage.jours.contains(Semaine.jour(jour, calendrier)),
                      let debut = calendrier.date(byAdding: .minute, value: plage.debut, to: jour),
                      debut > maintenant else {
                    continue
                }
                if prochain == nil || debut < (prochain ?? debut) {
                    prochain = debut
                }
                break
            }
        }
        return prochain
    }

    /// Heure à laquelle les apps se reverrouillent, si elles sont ouvertes.
    /// Une plage stricte coupe la fenêtre, mais pas le déblocage d'urgence.
    func finDOuverture(_ maintenant: Date, _ calendrier: Calendar) -> Date? {
        guard appsOuvertes(maintenant, calendrier) else { return nil }
        let urgence = urgenceEnCours(maintenant) ? urgenceFin : nil
        if plageActive(maintenant, calendrier) {
            return urgence
        }
        var fin = fenetreOuverte(maintenant) ? fenetreFin : nil
        if let fenetre = fin, let plage = prochainDebutDePlage(maintenant, calendrier), plage < fenetre {
            fin = plage
        }
        return [fin, urgence].compactMap { $0 }.max()
    }

    /// Démarre une fenêtre avec tout le solde. Si une fenêtre est déjà ouverte,
    /// elle est prolongée. Refusé pendant une plage stricte (règle 5).
    /// Renvoie la fin de la fenêtre, ou nil si rien n'a été ouvert.
    @discardableResult
    mutating func ouvrirFenetre(_ maintenant: Date, _ calendrier: Calendar) -> Date? {
        guard soldeMinutes > 0, !plageActive(maintenant, calendrier) else { return nil }
        let depart = fenetreOuverte(maintenant) ? (fenetreFin ?? maintenant) : maintenant
        let fin = depart.addingTimeInterval(TimeInterval(soldeMinutes * 60))
        fenetreFin = fin
        soldeMinutes = 0
        return fin
    }

    // MARK: Urgence

    /// Déblocages d'urgence encore possibles cette semaine. Remise à zéro le lundi (règle 6).
    func urgencesRestantes(_ maintenant: Date, _ calendrier: Calendar) -> Int {
        let lundi = Semaine.debut(maintenant, calendrier)
        let utilisees = urgences.filter { $0 >= lundi && $0 <= maintenant }.count
        return max(0, EtatJeu.urgencesParSemaine - utilisees)
    }

    /// Ouvre 5 minutes sans exercice. Passe même pendant une plage stricte.
    /// Renvoie la fin du déblocage, ou nil si l'urgence n'est pas disponible.
    @discardableResult
    mutating func utiliserUrgence(_ maintenant: Date, _ calendrier: Calendar) -> Date? {
        guard urgenceActivee, urgencesRestantes(maintenant, calendrier) > 0 else { return nil }
        let lundi = Semaine.debut(maintenant, calendrier)
        urgences = urgences.filter { $0 >= lundi }
        urgences.append(maintenant)
        let fin = maintenant.addingTimeInterval(TimeInterval(EtatJeu.minutesUrgence * 60))
        urgenceFin = fin
        return fin
    }

    // MARK: Série et stats

    /// Jours d'affilée avec au moins une séance validée (règle 8).
    /// La série tient si la dernière séance date d'aujourd'hui ou d'hier.
    func serie(_ maintenant: Date, _ calendrier: Calendar) -> Int {
        let jours = Set(seances.map { calendrier.startOfDay(for: $0.date) })
        var jour = calendrier.startOfDay(for: maintenant)
        if !jours.contains(jour) {
            guard let hier = calendrier.date(byAdding: .day, value: -1, to: jour), jours.contains(hier) else {
                return 0
            }
            jour = hier
        }
        var compte = 0
        while jours.contains(jour) {
            compte += 1
            guard let precedent = calendrier.date(byAdding: .day, value: -1, to: jour) else { break }
            jour = precedent
        }
        return compte
    }

    func repetitionsDuJour(_ maintenant: Date, _ calendrier: Calendar) -> Int {
        seances
            .filter { calendrier.isDate($0.date, inSameDayAs: maintenant) }
            .reduce(0) { $0 + $1.repetitions }
    }

    private func seancesDeLaSemaine(commencantLe lundi: Date, _ calendrier: Calendar) -> [Seance] {
        guard let fin = calendrier.date(byAdding: .day, value: 7, to: lundi) else { return [] }
        return seances.filter { $0.date >= lundi && $0.date < fin }
    }

    /// Bilan de la semaine en cours, du lundi au dimanche.
    func bilanDeLaSemaine(_ maintenant: Date, _ calendrier: Calendar) -> BilanSemaine {
        let lundi = Semaine.debut(maintenant, calendrier)
        let courantes = seancesDeLaSemaine(commencantLe: lundi, calendrier)
        var parJour = Array(repeating: 0, count: 7)
        for seance in courantes {
            parJour[Semaine.jour(seance.date, calendrier) - 1] += seance.repetitions
        }
        let total = parJour.reduce(0, +)
        var progression: Int?
        if let lundiPrecedent = calendrier.date(byAdding: .day, value: -7, to: lundi) {
            let avant = seancesDeLaSemaine(commencantLe: lundiPrecedent, calendrier)
                .reduce(0) { $0 + $1.repetitions }
            if avant > 0 {
                progression = Int((Double(total - avant) * 100 / Double(avant)).rounded())
            }
        }
        return BilanSemaine(
            repetitionsParJour: parJour,
            rangAujourdHui: Semaine.jour(maintenant, calendrier) - 1,
            seances: courantes.count,
            minutesGagnees: courantes.reduce(0) { $0 + $1.minutes },
            progressionPourcent: progression
        )
    }
}

/// Chiffres de l'écran Stats.
struct BilanSemaine: Equatable {
    /// Du lundi (rang 0) au dimanche (rang 6).
    var repetitionsParJour: [Int]
    var rangAujourdHui: Int
    var seances: Int
    var minutesGagnees: Int
    /// Évolution du total par rapport à la semaine précédente. Nil s'il n'y a pas de comparaison.
    var progressionPourcent: Int?

    var total: Int { repetitionsParJour.reduce(0, +) }
}
