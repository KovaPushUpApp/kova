import SwiftUI

/// Gabarit d'écran : contenu défilant, marges des tokens, boutons fixés en bas.
struct EcranKova<Contenu: View, Bas: View>: View {
    private let fond: Color
    private let espacement: CGFloat
    private let contenu: Contenu
    private let bas: Bas

    init(
        fond: Color = Tokens.Couleurs.fond,
        espacement: CGFloat = Tokens.Espacements.e21,
        @ViewBuilder contenu: () -> Contenu,
        @ViewBuilder bas: () -> Bas
    ) {
        self.fond = fond
        self.espacement = espacement
        self.contenu = contenu()
        self.bas = bas()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: espacement) {
                contenu
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Tokens.Ecran.margeCotes)
            .padding(.top, Tokens.Espacements.e8)
            .padding(.bottom, Tokens.Espacements.e21)
        }
        .rebondSelonContenu()
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: Tokens.Espacements.e5) {
                bas
            }
            .padding(.horizontal, Tokens.Ecran.margeCotes)
            .padding(.vertical, Tokens.Espacements.e8)
            .background(fond)
        }
        .background(fond.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}

extension EcranKova where Bas == EmptyView {
    /// Écran sans boutons fixés en bas (onglets).
    init(
        fond: Color = Tokens.Couleurs.fond,
        espacement: CGFloat = Tokens.Espacements.e21,
        @ViewBuilder contenu: () -> Contenu
    ) {
        self.init(fond: fond, espacement: espacement, contenu: contenu, bas: { EmptyView() })
    }
}

private extension View {
    /// Pas de rebond quand tout le contenu tient à l'écran (iOS 16.4 et plus).
    @ViewBuilder
    func rebondSelonContenu() -> some View {
        if #available(iOS 16.4, *) {
            scrollBounceBehavior(.basedOnSize)
        } else {
            self
        }
    }
}

extension View {
    /// Fond de carte arrondi.
    func carte(
        marge: CGFloat = Tokens.Espacements.e21,
        arrondi: CGFloat = Tokens.Arrondis.grand,
        fond: Color = Tokens.Couleurs.carte
    ) -> some View {
        padding(marge)
            .background(fond)
            .clipShape(RoundedRectangle(cornerRadius: arrondi, style: .continuous))
    }
}

/// En-tête d'onboarding : retour et barre d'avancement.
struct EnTeteEtapes: View {
    let etape: Int
    let total: Int
    let retour: () -> Void

    var body: some View {
        HStack(spacing: Tokens.Espacements.e13) {
            BoutonRetour(action: retour)
            GeometryReader { zone in
                ZStack(alignment: .leading) {
                    Capsule().fill(Tokens.Couleurs.ligne)
                    Capsule()
                        .fill(Tokens.Couleurs.accent)
                        .frame(width: zone.size.width * CGFloat(etape) / CGFloat(max(total, 1)))
                        .remplissageDepuisLaGauche()
                }
            }
            .frame(height: Tokens.Espacements.e5)
            .accessibilityElement()
            .accessibilityLabel(Text(verbatim: L.f("a11y.etape", etape, total)))
        }
    }
}

/// Ligne à choisir (question, exercice, formule) : bordure accent quand elle est choisie.
struct CarteOption<Contenu: View>: View {
    let choisie: Bool
    let action: () -> Void
    private let contenu: Contenu

    init(choisie: Bool, action: @escaping () -> Void, @ViewBuilder contenu: () -> Contenu) {
        self.choisie = choisie
        self.action = action
        self.contenu = contenu()
    }

    var body: some View {
        let forme = RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous)
        Button(action: action) {
            contenu
                .padding(.horizontal, Tokens.Espacements.e21)
                .frame(maxWidth: .infinity, minHeight: Tokens.Hauteurs.option, alignment: .leading)
                .background(choisie ? Tokens.Couleurs.carteSelectionnee : Tokens.Couleurs.carte)
                .clipShape(forme)
                .overlay(
                    forme.stroke(
                        choisie ? Tokens.Couleurs.accent : Tokens.Couleurs.ligne,
                        lineWidth: Tokens.Traits.normal
                    )
                )
                .contentShape(forme)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(choisie ? .isSelected : [])
    }
}

/// Interrupteur de la maquette : 55 x 34, pastille de 28.
struct Interrupteur: View {
    let actif: Bool
    let libelle: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Circle()
                .fill(actif ? Tokens.Couleurs.texteSurAccent : Tokens.Couleurs.texteSecondaire)
                .frame(
                    width: Tokens.Composants.Interrupteur.pastille,
                    height: Tokens.Composants.Interrupteur.pastille
                )
                .offset(x: actif ? Tokens.Composants.Interrupteur.decalageActif : 0)
                .padding(Tokens.Espacements.e3)
                .frame(
                    width: Tokens.Composants.Interrupteur.largeur,
                    height: Tokens.Composants.Interrupteur.hauteur,
                    alignment: .leading
                )
                .background(actif ? Tokens.Couleurs.accent : Tokens.Couleurs.ligne)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.moyen, style: .continuous))
                .animation(Mouvement.courbe(), value: actif)
                // Zone tactile de 44 au minimum.
                .frame(minHeight: Tokens.Hauteurs.boutonIcone)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(verbatim: libelle))
        .accessibilityValue(Text(verbatim: L.t(actif ? "a11y.active" : "a11y.desactive")))
        .accessibilityAddTraits(.isButton)
    }
}

/// Ligne "coche + texte" des listes d'arguments.
struct LigneCoche: View {
    let texte: String

    var body: some View {
        HStack(alignment: .center, spacing: Tokens.Espacements.e13) {
            Icone(nom: .coche, couleur: Tokens.Couleurs.accent)
            Text(verbatim: texte).corps()
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

/// Icône dans un carré arrondi, avec l'anneau qui s'élargit (autorisations, verrou).
struct IconeAnneau: View {
    let icone: Icone.Nom
    var avecAnneau = true

    var body: some View {
        ZStack {
            if avecAnneau {
                Anneau()
            }
            Icone(nom: icone, taille: Tokens.Espacements.e34, couleur: Tokens.Couleurs.accent)
                .frame(width: Tokens.Composants.Logo.taille, height: Tokens.Composants.Logo.taille)
                .background(Tokens.Couleurs.carte)
                .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.tresGrand, style: .continuous))
                .pop()
        }
        .frame(width: Tokens.Composants.Logo.taille, height: Tokens.Composants.Logo.taille)
        .frame(maxWidth: .infinity)
        .padding(.top, Tokens.Espacements.e55)
    }
}

/// Emplacement de la vue caméra, avec la ligne de scan.
/// À l'étape 3, l'image réelle de la caméra remplace la silhouette.
struct VueCameraFictive: View {
    let hauteur: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var enBas = false

    var body: some View {
        let forme = RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous)
        let debut = hauteur * Tokens.Animations.ScanCamera.debut
        let fin = hauteur * Tokens.Animations.ScanCamera.fin
        ZStack(alignment: .top) {
            Tokens.Couleurs.carte
            VStack(spacing: Tokens.Espacements.e13) {
                FigurePompe()
                Text(verbatim: L.t("camera.fictive")).legende()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            Rectangle()
                .fill(Tokens.Couleurs.accent)
                .opacity(Double(Tokens.Opacites.scan))
                .frame(height: Tokens.Traits.normal)
                .offset(y: enBas ? fin : debut)
                .opacity(reduire ? 0 : 1)
                .accessibilityHidden(true)
        }
        .frame(height: hauteur)
        .frame(maxWidth: .infinity)
        .clipShape(forme)
        .overlay(
            forme.stroke(
                Tokens.Couleurs.pointille,
                style: StrokeStyle(lineWidth: Tokens.Traits.fin, dash: [Tokens.Espacements.e5, Tokens.Espacements.e5])
            )
        )
        .onAppear {
            guard !reduire else { return }
            withAnimation(
                .linear(duration: Double(Tokens.Animations.ScanCamera.duree) / 2)
                    .repeatForever(autoreverses: true)
            ) {
                enBas = true
            }
        }
    }
}

/// Barre de progression à segments de la séance.
struct BarreSegments: View {
    let faits: Int
    let total: Int

    var body: some View {
        HStack(spacing: Tokens.Composants.BarreProgression.ecart) {
            ForEach(0..<max(total, 1), id: \.self) { rang in
                Capsule()
                    .fill(rang < faits ? Tokens.Couleurs.accent : Tokens.Couleurs.ligne)
                    .frame(height: Tokens.Composants.BarreProgression.hauteur)
                    .animation(Mouvement.courbe(), value: faits)
            }
        }
        .accessibilityElement()
        .accessibilityLabel(Text(verbatim: L.t("a11y.progression")))
        .accessibilityValue(Text(verbatim: L.f("a11y.progression_valeur", faits, total)))
    }
}

/// Groupe de lignes de réglages : carte arrondie, lignes séparées par un filet.
struct GroupeReglages<Contenu: View>: View {
    private let contenu: Contenu

    init(@ViewBuilder contenu: () -> Contenu) {
        self.contenu = contenu()
    }

    var body: some View {
        VStack(spacing: 0) {
            contenu
        }
        .background(Tokens.Couleurs.carte)
        .clipShape(RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous))
    }
}

/// Ligne de réglage : libellé à gauche, valeur ou interrupteur à droite.
struct LigneReglage<Droite: View>: View {
    let libelle: String
    private let droite: Droite

    init(_ libelle: String, @ViewBuilder droite: () -> Droite) {
        self.libelle = libelle
        self.droite = droite()
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: Tokens.Espacements.e13) {
                Text(verbatim: libelle).corps()
                Spacer(minLength: Tokens.Espacements.e8)
                droite
            }
            .padding(.horizontal, Tokens.Espacements.e13)
            .frame(minHeight: Tokens.Hauteurs.ligneReglage)
            Rectangle()
                .fill(Tokens.Couleurs.ligneFine)
                .frame(height: Tokens.Traits.fin)
        }
    }
}

/// Valeur d'une ligne de réglage suivie d'un chevron.
struct ValeurAvecChevron: View {
    let valeur: String

    var body: some View {
        HStack(spacing: Tokens.Espacements.e5) {
            Text(verbatim: valeur).legende()
            Icone(nom: .chevronDroit, taille: Tokens.Espacements.e13, couleur: Tokens.Couleurs.texteSecondaire)
        }
    }
}

/// Ligne de réglage qui ouvre un autre écran.
struct LigneReglageLien: View {
    let libelle: String
    var valeur: String = ""
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            LigneReglage(libelle) {
                ValeurAvecChevron(valeur: valeur)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

/// Ligne d'une app verrouillée. À l'étape 4, le nom et l'icône viennent du
/// composant Label d'Apple (les apps choisies sont des jetons opaques).
struct LigneApp: View {
    let nom: String

    var body: some View {
        HStack(spacing: Tokens.Espacements.e13) {
            RoundedRectangle(cornerRadius: Tokens.Composants.VignetteApp.arrondi, style: .continuous)
                .fill(Tokens.Couleurs.ligne)
                .frame(width: Tokens.Composants.VignetteApp.taille, height: Tokens.Composants.VignetteApp.taille)
                .accessibilityHidden(true)
            Text(verbatim: nom).corps(graisse: .demiGras)
            Spacer(minLength: Tokens.Espacements.e8)
            Icone(nom: .cadenas, couleur: Tokens.Couleurs.texteSecondaire)
        }
        .carte(marge: Tokens.Espacements.e13, arrondi: Tokens.Arrondis.moyen)
    }
}

/// Bouton en pointillés ("Ajouter une plage", "Choisir mes apps").
struct BoutonPointille: View {
    let titre: String
    let action: () -> Void

    var body: some View {
        let forme = RoundedRectangle(cornerRadius: Tokens.Arrondis.grand, style: .continuous)
        Button(action: action) {
            Text(verbatim: titre)
                .corps(graisse: .demiGras)
                .frame(maxWidth: .infinity, minHeight: Tokens.Hauteurs.boutonPrincipal)
                .overlay(
                    forme.stroke(
                        Tokens.Couleurs.pointille,
                        style: StrokeStyle(lineWidth: Tokens.Traits.fin, dash: [Tokens.Espacements.e5, Tokens.Espacements.e5])
                    )
                )
                .contentShape(forme)
        }
        .buttonStyle(.plain)
    }
}
