import SwiftUI

/// Écran 21 : les réglages, en trois groupes (règles, options, compte).
struct EcranReglages: View {
    @EnvironmentObject private var etat: EtatApp
    let ouvrir: (Destination) -> Void

    // Options fictives à l'étape 2. Leur effet arrive aux étapes 5 et 7.
    @State private var urgenceActive = true
    @State private var rappelQuotidien = true
    @State private var compterAVoixHaute = false

    var body: some View {
        EcranKova(espacement: Tokens.Espacements.e13) {
            Text(verbatim: L.t("reglages.titre")).titreEcran()
            GroupeReglages {
                LigneReglageLien(
                    libelle: L.t("ecran.apps_reglage"),
                    valeur: L.n("reglages.apps", etat.apps.count),
                    action: { ouvrir(.appsReglage) }
                )
                LigneReglageLien(
                    libelle: L.t("reglages.regle"),
                    valeur: L.f("reglages.regle.valeur", etat.regle.repetitions, etat.regle.minutes),
                    action: { ouvrir(.regle) }
                )
                LigneReglageLien(
                    libelle: L.t("reglages.exercice"),
                    valeur: etat.exercice.nom,
                    action: { ouvrir(.exercice) }
                )
            }
            .entree(rang: 1)
            GroupeReglages {
                LigneReglage(L.t("ecran.urgence")) {
                    Interrupteur(actif: urgenceActive, libelle: L.t("ecran.urgence")) {
                        urgenceActive.toggle()
                    }
                }
                LigneReglage(L.t("reglages.rappel")) {
                    Interrupteur(actif: rappelQuotidien, libelle: L.t("reglages.rappel")) {
                        rappelQuotidien.toggle()
                    }
                }
                LigneReglage(L.t("reglages.voix")) {
                    Interrupteur(actif: compterAVoixHaute, libelle: L.t("reglages.voix")) {
                        compterAVoixHaute.toggle()
                    }
                }
            }
            .entree(rang: 2)
            GroupeReglages {
                LigneReglageLien(
                    libelle: L.t("reglages.abonnement"),
                    valeur: L.t("formule.annuel"),
                    action: { ouvrir(.abonnement) }
                )
                LigneReglageLien(libelle: L.t("reglages.aide"), action: {})
                LigneReglageLien(libelle: L.t("legal.confidentialite"), action: {})
            }
            .entree(rang: 3)
            // Accès de chantier, retirés à l'étape 7.
            BoutonSecondaire(L.t("socle.titre")) { ouvrir(.controleSocle) }
            BoutonSecondaire(L.t("socle.revoir_onboarding")) { etat.onboardingTermine = false }
        }
    }
}
