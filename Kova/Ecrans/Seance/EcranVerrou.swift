import SwiftUI

/// Écran 13, version dans l'app. Le vrai écran de verrou est dessiné par le système
/// par dessus l'app bloquée, avec un titre, un sous-titre et deux boutons au plus
/// (extension KovaShieldConfig). Celui-ci est l'écran sur lequel on arrive dans
/// Kova après avoir touché "Faire mes pompes". Il part de la maquette et
/// l'enrichit, comme sur Android : temps en stock utilisable tout de suite,
/// série et urgences restantes.
/// Le nom de l'app bloquée n'est pas connu de Kova, d'où "Tes apps".
struct EcranVerrou: View {
    @EnvironmentObject private var etat: EtatApp
    let commencer: () -> Void
    let fermer: () -> Void
    let urgence: () -> Void

    var body: some View {
        let maintenant = Date()
        let stock = etat.jeu.soldeMinutes
        // Pendant une plage stricte, le stock ne s'utilise pas.
        let peutOuvrir = stock > 0 && !etat.jeu.plageActive(maintenant, etat.calendrier)
        let urgences = etat.jeu.urgencesRestantes(maintenant, etat.calendrier)
        let urgencePossible = etat.jeu.urgenceActivee && urgences > 0
        EcranKova {
            IconeAnneau(icone: .cadenas)
            Text(verbatim: L.t("verrou.app.titre"))
                .titreEcran()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, Tokens.Espacements.e13)
                .entree(rang: 2)
            Text(verbatim: peutOuvrir ? L.f("verrou.app.stock", stock) : L.t("verrou.app.texte"))
                .paragraphe()
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .entree(rang: 3)
            HStack {
                bloc(
                    L.f("verrou.app.pompes", etat.regle.repetitions),
                    L.t("verrou.app.a_faire"),
                    couleur: Tokens.Couleurs.texte,
                    alignement: .leading
                )
                Spacer(minLength: Tokens.Espacements.e8)
                Icone(nom: .fleche, couleur: Tokens.Couleurs.texteSecondaire)
                Spacer(minLength: Tokens.Espacements.e8)
                bloc(
                    L.f("commun.minutes_court", etat.regle.minutes),
                    L.t("verrou.app.debloquees"),
                    couleur: Tokens.Couleurs.accent,
                    alignement: .trailing
                )
            }
            .carte()
            .padding(.top, Tokens.Espacements.e13)
            .entree(rang: 4)
            // Trois repères d'un coup d'œil : le temps en stock, la série, les urgences.
            HStack(alignment: .top, spacing: Tokens.Espacements.e8) {
                repere(L.f("commun.minutes_court", stock), L.t("verrou.app.stock_legende"))
                repere(
                    L.n("commun.jours", etat.jeu.serie(maintenant, etat.calendrier)),
                    L.t("verrou.app.serie")
                )
                repere(
                    L.f("urgence.compte", urgences, EtatJeu.urgencesParSemaine),
                    L.t("verrou.app.urgences")
                )
            }
            .fixedSize(horizontal: false, vertical: true)
            .entree(rang: 5)
        } bas: {
            if peutOuvrir {
                BoutonPrincipal(L.f("verrou.app.utiliser", stock)) {
                    etat.ouvrirFenetre()
                    fermer()
                }
                .entree(rang: 6)
                BoutonSecondaire(L.t("verrou.app.gagner_plus"), couleur: Tokens.Couleurs.texte, action: commencer)
            } else {
                BoutonPrincipal(L.t("verrou.app.commencer"), action: commencer)
                    .entree(rang: 6)
            }
            BoutonSecondaire(L.t("verrou.app.plus_tard"), action: fermer)
            if urgencePossible {
                BoutonSecondaire(L.t("ecran.urgence"), action: urgence)
            }
        }
    }

    private func bloc(
        _ valeur: String,
        _ libelle: String,
        couleur: Color,
        alignement: HorizontalAlignment
    ) -> some View {
        VStack(alignment: alignement, spacing: Tokens.Espacements.e3) {
            Text(verbatim: valeur)
                .font(.kovaTitre(Tokens.TaillesTexte.titreEcran))
                .foregroundColor(couleur)
                .lineLimit(1)
                .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
            Text(verbatim: libelle).legende()
        }
    }

    /// Petit repère : une valeur et sa légende.
    private func repere(_ valeur: String, _ libelle: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e3) {
            Text(verbatim: valeur)
                .font(.kovaTitre(Tokens.TaillesTexte.corpsFort))
                .foregroundColor(Tokens.Couleurs.texte)
                .lineLimit(1)
                .minimumScaleFactor(Tokens.Typographie.echelleMinimale)
            Text(verbatim: libelle).legende()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .carte(marge: Tokens.Espacements.e13)
    }
}
