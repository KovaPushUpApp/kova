import FamilyControls
import ManagedSettings
import SwiftUI

/// La liste des apps verrouillées. Le nom et l'icône de chaque app sont
/// dessinés par le composant Label d'Apple, à partir d'un jeton opaque.
struct VueAppsChoisies: View {
    @ObservedObject var apps: GestionApps

    var body: some View {
        VStack(spacing: Tokens.Espacements.e8) {
            if Verrouillage.disponible {
                ForEach(Array(apps.selection.applicationTokens), id: \.self) { jeton in
                    ligne { Label(jeton) }
                }
                ForEach(Array(apps.selection.categoryTokens), id: \.self) { jeton in
                    ligne { Label(jeton) }
                }
                ForEach(Array(apps.selection.webDomainTokens), id: \.self) { jeton in
                    ligne { Label(jeton) }
                }
            } else {
                ForEach(apps.fictives, id: \.self) { nom in
                    LigneApp(nom: nom)
                }
            }
        }
    }

    private func ligne<Etiquette: View>(@ViewBuilder _ etiquette: () -> Etiquette) -> some View {
        HStack(spacing: Tokens.Espacements.e13) {
            etiquette()
                .labelStyle(.titleAndIcon)
                .font(.kovaTexte(graisse: .demiGras))
                .foregroundColor(Tokens.Couleurs.texte)
                .lineLimit(1)
            Spacer(minLength: Tokens.Espacements.e8)
            Icone(nom: .cadenas, couleur: Tokens.Couleurs.texteSecondaire)
        }
        .carte(marge: Tokens.Espacements.e13, arrondi: Tokens.Arrondis.moyen)
    }
}

/// Bouton "Choisir mes apps" qui ouvre le sélecteur système d'Apple, puis la
/// liste des apps choisies. Commun à l'onboarding et aux réglages.
struct SelecteurApps: View {
    @ObservedObject var apps: GestionApps
    /// Appelé quand l'utilisateur a validé un nouveau choix dans le sélecteur.
    let surChoix: (FamilyActivitySelection) -> Void

    @State private var ouvert = false
    @State private var brouillon = FamilyActivitySelection()

    var body: some View {
        VStack(spacing: Tokens.Espacements.e8) {
            BoutonPointille(titre: L.t("apps.choisir")) {
                // Dans le build sans verrouillage, le sélecteur d'Apple n'est pas utilisable.
                guard Verrouillage.disponible else { return }
                brouillon = apps.selection
                ouvert = true
            }
            VueAppsChoisies(apps: apps)
        }
        .familyActivityPicker(isPresented: $ouvert, selection: $brouillon)
        .onChange(of: ouvert) { estOuvert in
            // Le sélecteur vient de se fermer : on prend son résultat.
            if !estOuvert, brouillon != apps.selection {
                surChoix(brouillon)
            }
        }
    }
}
