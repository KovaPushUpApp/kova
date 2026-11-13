import SwiftUI

/// Courbes et durées communes. Toutes les valeurs viennent des tokens.
enum Mouvement {
    /// Courbe "ease" du web : cubic-bezier(0.25, 0.1, 0.25, 1).
    static func courbe(_ duree: CGFloat = Tokens.Animations.dureeStandard) -> Animation {
        .timingCurve(0.25, 0.1, 0.25, 1, duration: Double(duree))
    }

    /// Décalage d'entrée d'un bloc selon son rang dans la cascade.
    static func delai(rang: Int) -> Double {
        Double(rang) * Double(Tokens.Animations.Entree.cascade[0])
    }
}

/// Entrée d'un bloc : fondu et montée de 18 points.
private struct Entree: ViewModifier {
    let rang: Int
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var visible = false

    func body(content: Content) -> some View {
        content
            .opacity(Double(visible || reduire ? Tokens.Animations.Entree.opacite[1] : Tokens.Animations.Entree.opacite[0]))
            .offset(y: visible || reduire ? Tokens.Animations.Entree.translationY[1] : Tokens.Animations.Entree.translationY[0])
            .onAppear {
                withAnimation(Mouvement.courbe().delay(Mouvement.delai(rang: rang))) {
                    visible = true
                }
            }
    }
}

/// Apparition "pop" : échelle 0,6 vers 1,06 puis 1.
private struct Pop: ViewModifier {
    let rang: Int
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var echelle = Tokens.Animations.Pop.echelle[0]
    @State private var opacite: Double = 0

    func body(content: Content) -> some View {
        content
            .scaleEffect(reduire ? Tokens.Animations.Pop.echelle[2] : echelle)
            .opacity(reduire ? 1 : opacite)
            .onAppear {
                let delai = Mouvement.delai(rang: rang)
                let montee = Tokens.Animations.Pop.duree * Tokens.Animations.Pop.pic
                let retombee = Tokens.Animations.Pop.duree - montee
                withAnimation(Mouvement.courbe(montee).delay(delai)) {
                    echelle = Tokens.Animations.Pop.echelle[1]
                    opacite = 1
                }
                withAnimation(Mouvement.courbe(retombee).delay(delai + Double(montee))) {
                    echelle = Tokens.Animations.Pop.echelle[2]
                }
            }
    }
}

/// Pulsation lente et continue des boutons principaux.
private struct Pulsation: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var gonfle = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(gonfle && !reduire ? Tokens.Animations.Pulsation.echelle[1] : Tokens.Animations.Pulsation.echelle[0])
            .onAppear {
                guard !reduire else { return }
                withAnimation(
                    .easeInOut(duration: Double(Tokens.Animations.Pulsation.duree) / 2)
                        .repeatForever(autoreverses: true)
                ) {
                    gonfle = true
                }
            }
    }
}

/// Croissance d'une barre depuis un bord (bas pour les stats, gauche pour les jauges).
private struct Croissance: ViewModifier {
    let ancre: UnitPoint
    let duree: CGFloat
    let delai: Double
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var pleine = false

    func body(content: Content) -> some View {
        let facteur: CGFloat = pleine || reduire ? 1 : 0
        content
            .scaleEffect(
                x: ancre == .leading ? facteur : 1,
                y: ancre == .bottom ? facteur : 1,
                anchor: ancre
            )
            .onAppear {
                withAnimation(Mouvement.courbe(duree).delay(delai)) {
                    pleine = true
                }
            }
    }
}

extension View {
    /// Fondu et montée. `rang` place le bloc dans la cascade.
    func entree(rang: Int = 0) -> some View {
        modifier(Entree(rang: rang))
    }

    /// Apparition "pop" pour le logo, les cadenas et les gros chiffres.
    func pop(rang: Int = 0) -> some View {
        modifier(Pop(rang: rang))
    }

    /// Pulsation lente du bouton principal.
    func pulsation() -> some View {
        modifier(Pulsation())
    }

    /// Barre de stats qui pousse depuis le bas.
    func croissanceDepuisLeBas(rang: Int = 0) -> some View {
        modifier(Croissance(
            ancre: .bottom,
            duree: Tokens.Animations.CroissanceBarre.duree,
            delai: Mouvement.delai(rang: rang)
        ))
    }

    /// Jauge qui se remplit depuis la gauche.
    func remplissageDepuisLaGauche(rang: Int = 0) -> some View {
        modifier(Croissance(
            ancre: .leading,
            duree: Tokens.Animations.Remplissage.duree,
            delai: Mouvement.delai(rang: rang)
        ))
    }
}

/// Anneau qui s'élargit en boucle autour d'une icône.
struct Anneau: View {
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var ouvert = false

    var body: some View {
        Circle()
            .stroke(Tokens.Couleurs.accent, lineWidth: Tokens.Traits.normal)
            .scaleEffect(ouvert ? Tokens.Animations.Anneau.echelle[1] : Tokens.Animations.Anneau.echelle[0])
            .opacity(Double(ouvert ? Tokens.Animations.Anneau.opacite[1] : Tokens.Animations.Anneau.opacite[0]))
            .opacity(reduire ? 0 : 1)
            .onAppear {
                guard !reduire else { return }
                withAnimation(
                    .easeOut(duration: Double(Tokens.Animations.Anneau.duree))
                        .repeatForever(autoreverses: false)
                ) {
                    ouvert = true
                }
            }
            .accessibilityHidden(true)
    }
}
