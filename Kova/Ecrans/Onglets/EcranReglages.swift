import SwiftUI

/// Écran 21 : les réglages, en trois groupes (règles, options, compte).
struct EcranReglages: View {
    @EnvironmentObject private var etat: EtatApp
    @EnvironmentObject private var apps: GestionApps
    @EnvironmentObject private var abonnements: Abonnements
    @Environment(\.openURL) private var ouvrirLien
    let ouvrir: (Destination) -> Void

    private var formule: String {
        switch abonnements.formuleActuelle {
        case .annuel: return L.t("formule.annuel")
        case .hebdomadaire: return L.t("formule.hebdomadaire")
        case .none: return abonnements.estAbonne ? "" : L.t("formule.gratuite")
        }
    }

    private var regle: String {
        let texte = L.f("reglages.regle.valeur", etat.regle.repetitions, etat.regle.minutes)
        return etat.jeu.regleEnAttente == nil ? texte : L.f("reglages.regle.en_attente", texte)
    }

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            Text(verbatim: L.t("reglages.titre")).titreEcran()
            GroupeReglages {
                LigneReglageLien(
                    libelle: L.t("ecran.apps_reglage"),
                    valeur: L.n("reglages.apps", apps.nombre),
                    action: { ouvrir(.appsReglage) }
                )
                LigneReglageLien(libelle: L.t("reglages.regle"), valeur: regle, action: { ouvrir(.regle) })
                LigneReglageLien(
                    libelle: L.t("reglages.exercice"),
                    valeur: etat.jeu.exerciceParDefaut.nom,
                    action: { ouvrir(.exerciceParDefaut) }
                )
            }
            .entree(rang: 1)
            GroupeReglages {
                LigneReglage(L.t("ecran.urgence")) {
                    Interrupteur(actif: etat.jeu.urgenceActivee, libelle: L.t("ecran.urgence")) {
                        etat.definirUrgenceActivee(!etat.jeu.urgenceActivee)
                    }
                }
                LigneReglage(L.t("reglages.rappel")) {
                    Interrupteur(actif: etat.rappelQuotidien, libelle: L.t("reglages.rappel")) {
                        etat.rappelQuotidien.toggle()
                    }
                }
                LigneReglage(L.t("reglages.voix")) {
                    Interrupteur(actif: etat.compterAVoixHaute, libelle: L.t("reglages.voix")) {
                        etat.compterAVoixHaute.toggle()
                    }
                }
                LigneReglage(L.t("reglages.mesure")) {
                    Interrupteur(actif: etat.mesureAcceptee, libelle: L.t("reglages.mesure")) {
                        etat.mesureAcceptee.toggle()
                    }
                }
            }
            .entree(rang: 2)
            GroupeReglages {
                LigneReglageLien(
                    libelle: L.t("reglages.abonnement"),
                    valeur: formule,
                    action: { ouvrir(.abonnement) }
                )
                if let contact = Liens.contact {
                    LigneReglageLien(libelle: L.t("reglages.aide"), action: { ouvrirLien(contact) })
                }
                if let confidentialite = Liens.confidentialite {
                    LigneReglageLien(libelle: L.t("legal.confidentialite"), action: { ouvrirLien(confidentialite) })
                }
                if let conditions = Liens.conditions {
                    LigneReglageLien(libelle: L.t("legal.conditions_longues"), action: { ouvrirLien(conditions) })
                }
            }
            .entree(rang: 3)
            // Accès de chantier : seulement dans le build de test sans verrouillage
            // (project.base.yml). Le projet complet, celui qui part en revue, ne l'a pas.
            #if KOVA_SANS_VERROUILLAGE
            BoutonSecondaire(L.t("socle.titre")) { ouvrir(.controleSocle) }
            #endif
        }
    }
}
