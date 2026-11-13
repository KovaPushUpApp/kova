import SwiftUI

/// Mise en forme d'une plage pour l'affichage.
extension Plage {
    private static func heure(_ minutes: Int) -> String {
        var composants = DateComponents()
        composants.hour = minutes / 60
        composants.minute = minutes % 60
        let date = Calendar.current.date(from: composants) ?? Date()
        return date.formatted(date: .omitted, time: .shortened)
    }

    /// "9:00 à 18:00".
    var horaires: String {
        L.f("planning.horaires", Plage.heure(debut), Plage.heure(fin))
    }

    /// "Tous les jours", "Du lundi au vendredi", ou la liste des jours.
    var libelleJours: String {
        if jours == Plage.tousLesJours {
            return L.t("planning.jours.tous")
        }
        if jours == [1, 2, 3, 4, 5] {
            return L.t("planning.jours.semaine")
        }
        if jours == [6, 7] {
            return L.t("planning.jours.weekend")
        }
        let symboles = Calendar.current.shortStandaloneWeekdaySymbols
        guard symboles.count == 7 else { return "" }
        // Les symboles du système commencent le dimanche.
        return jours.sorted().map { symboles[$0 % 7] }.joined(separator: ", ")
    }
}

/// Écran 20 : les plages strictes. Pendant une plage active, les apps restent
/// verrouillées même avec du solde (brief, règle 5). Activer est immédiat,
/// désactiver prend effet 24 h plus tard (règle 7).
struct EcranPlanning: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var abonnements: Abonnements
    let ouvrirPaywall: () -> Void

    @State private var ajoutOuvert = false

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            Text(verbatim: L.t("planning.titre")).titreEcran()
            Text(verbatim: L.t("planning.texte"))
                .paragraphe()
                .entree(rang: 1)
            ForEach(Array(etat.jeu.plages.enumerated()), id: \.element.id) { rang, plage in
                carte(plage)
                    .entree(rang: 2 + rang)
            }
            if etat.jeu.plages.count < Plage.nombreMaximum {
                BoutonPointille(titre: L.t("planning.ajouter")) {
                    // Le planning fait partie de l'abonnement (brief, section 4).
                    if abonnements.estAbonne {
                        ajoutOuvert = true
                    } else {
                        ouvrirPaywall()
                    }
                }
                .entree(rang: 6)
            }
        }
        .sheet(isPresented: $ajoutOuvert) {
            EditeurPlage(
                annuler: { ajoutOuvert = false },
                ajouter: { plage in
                    etat.ajouterPlage(plage)
                    ajoutOuvert = false
                }
            )
            .preferredColorScheme(.dark)
        }
    }

    private func carte(_ plage: Plage) -> some View {
        let enSursis = !plage.active && plage.enVigueur(Date())
        return HStack(spacing: Tokens.Espacements.e13) {
            VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
                Text(verbatim: plage.nom).corps(graisse: .demiGras)
                Text(verbatim: plage.horaires).titreCarte()
                Text(verbatim: enSursis ? L.t("planning.sursis") : plage.libelleJours)
                    .legende(enSursis ? Tokens.Couleurs.accent : Tokens.Couleurs.texteSecondaire)
            }
            Spacer(minLength: Tokens.Espacements.e8)
            Interrupteur(
                actif: plage.active,
                libelle: L.f("a11y.plage", plage.nom),
                action: { etat.basculerPlage(plage.id) }
            )
        }
        .carte()
    }
}

/// Feuille d'ajout d'une plage : nom, heures et jours.
struct EditeurPlage: View {
    let annuler: () -> Void
    let ajouter: (Plage) -> Void

    @State private var nom = ""
    @State private var debut = EditeurPlage.heure(9)
    @State private var fin = EditeurPlage.heure(18)
    @State private var jours: Set<Int> = [1, 2, 3, 4, 5]

    private static func heure(_ heure: Int) -> Date {
        Calendar.current.date(bySettingHour: heure, minute: 0, second: 0, of: Date()) ?? Date()
    }

    private static func minutes(_ date: Date) -> Int {
        let composants = Calendar.current.dateComponents([.hour, .minute], from: date)
        return (composants.hour ?? 0) * 60 + (composants.minute ?? 0)
    }

    /// Initiales des jours, du lundi au dimanche.
    private var initiales: [String] {
        let symboles = Calendar.current.veryShortStandaloneWeekdaySymbols
        guard symboles.count == 7 else { return Array(repeating: "", count: 7) }
        return Array(symboles[1...]) + [symboles[0]]
    }

    private var valide: Bool {
        !jours.isEmpty && EditeurPlage.minutes(debut) != EditeurPlage.minutes(fin)
    }

    var body: some View {
        EcranKova {
            HStack {
                Text(verbatim: L.t("planning.ajouter")).titreCarte()
                Spacer()
                BoutonFermer(action: annuler)
            }
            TextField("", text: $nom, prompt: Text(verbatim: L.t("planning.editeur.nom")))
                .font(.kovaTexte())
                .foregroundColor(Tokens.Couleurs.texte)
                .padding(.horizontal, Tokens.Espacements.e13)
                .frame(minHeight: Tokens.Hauteurs.option)
                .background(Tokens.Couleurs.carte)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
                .accessibilityLabel(Text(verbatim: L.t("planning.editeur.nom")))
            GroupeReglages {
                LigneReglage(L.t("planning.editeur.debut")) {
                    DatePicker("", selection: $debut, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                LigneReglage(L.t("planning.editeur.fin")) {
                    DatePicker("", selection: $fin, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
            }
            HStack(spacing: Tokens.Espacements.e5) {
                ForEach(1...7, id: \.self) { jour in
                    boutonJour(jour)
                }
            }
            Text(verbatim: L.t("planning.editeur.note")).paragraphe()
        } bas: {
            BoutonPrincipal(L.t("planning.editeur.bouton")) {
                guard valide else { return }
                let propre = nom.trimmingCharacters(in: .whitespacesAndNewlines)
                ajouter(Plage(
                    nom: propre.isEmpty ? L.t("planning.editeur.sans_nom") : propre,
                    debut: EditeurPlage.minutes(debut),
                    fin: EditeurPlage.minutes(fin),
                    jours: jours
                ))
            }
            .opacity(valide ? 1 : Double(Tokens.Opacites.scan))
        }
    }

    private func boutonJour(_ jour: Int) -> some View {
        let choisi = jours.contains(jour)
        return Button {
            if choisi {
                jours.remove(jour)
            } else {
                jours.insert(jour)
            }
        } label: {
            Text(verbatim: initiales[jour - 1])
                .legende(choisi ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texteSecondaire, graisse: .demiGras)
                .frame(maxWidth: .infinity, minHeight: Tokens.Hauteurs.boutonIcone)
                .background(choisi ? Tokens.Couleurs.accent : Tokens.Couleurs.carte)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(verbatim: Calendar.current.standaloneWeekdaySymbols[jour % 7]))
        .accessibilityAddTraits(choisi ? .isSelected : [])
    }
}
