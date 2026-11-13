import SwiftUI

/// Gabarit d'attente utilisé par le socle : un titre et les boutons de navigation.
/// Chaque écran le remplace par sa vraie vue à l'étape 2.
struct EcranProvisoire<Actions: View>: View {
    private let titre: LocalizedStringKey
    private let retour: (() -> Void)?
    private let actions: Actions

    init(
        titre: LocalizedStringKey,
        retour: (() -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.titre = titre
        self.retour = retour
        self.actions = actions()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.Espacements.e21) {
            if let retour {
                BoutonRetour(action: retour)
            }
            Text(titre)
                .font(.kovaTitre(Tokens.TaillesTexte.titreEcran))
                .foregroundColor(Tokens.Couleurs.texte)
                .accessibilityAddTraits(.isHeader)
            Text("socle.ecran_provisoire")
                .font(.kovaTexte())
                .lineSpacing(Tokens.TaillesTexte.corps * (Tokens.Interligne.paragraphe - 1))
                .foregroundColor(Tokens.Couleurs.texteSecondaire)
            Spacer(minLength: Tokens.Espacements.e21)
            VStack(spacing: Tokens.Espacements.e8) {
                actions
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(.horizontal, Tokens.Ecran.margeCotes)
        .padding(.top, Tokens.Espacements.e8)
        .padding(.bottom, Tokens.Espacements.e13)
        .background(Tokens.Couleurs.fond.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}
