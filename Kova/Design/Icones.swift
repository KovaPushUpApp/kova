import SwiftUI

// Tracés repris des maquettes (SVG). Les coordonnées sont celles des fichiers
// .dc.html : elles décrivent la forme, pas une valeur de design.

/// Constante de Bézier pour dessiner un quart de cercle.
private let kappa: CGFloat = 0.5523

private extension Path {
    /// Demi-cercle passant par le haut, de la gauche vers la droite.
    mutating func demiCercleHaut(centre: CGPoint, rayon r: CGFloat) {
        let k = r * kappa
        addCurve(
            to: CGPoint(x: centre.x, y: centre.y - r),
            control1: CGPoint(x: centre.x - r, y: centre.y - k),
            control2: CGPoint(x: centre.x - k, y: centre.y - r)
        )
        addCurve(
            to: CGPoint(x: centre.x + r, y: centre.y),
            control1: CGPoint(x: centre.x + k, y: centre.y - r),
            control2: CGPoint(x: centre.x + r, y: centre.y - k)
        )
    }

    mutating func trait(_ x1: CGFloat, _ y1: CGFloat, _ x2: CGFloat, _ y2: CGFloat) {
        move(to: CGPoint(x: x1, y: y1))
        addLine(to: CGPoint(x: x2, y: y2))
    }

    mutating func ligneBrisee(_ points: [(CGFloat, CGFloat)]) {
        guard let premier = points.first else { return }
        move(to: CGPoint(x: premier.0, y: premier.1))
        for point in points.dropFirst() {
            addLine(to: CGPoint(x: point.0, y: point.1))
        }
    }
}

/// Pictogramme au trait, dessiné sur une grille de 24.
struct Icone: View {
    enum Nom {
        case chevronGauche, chevronDroit, croix, coche, cadenas, camera, eclair, fleche
        case maison, stats, calendrier, reglages
    }

    let nom: Nom
    var taille: CGFloat = Tokens.Espacements.e21
    var couleur: Color = Tokens.Couleurs.texte

    var body: some View {
        TraceIcone(nom: nom)
            .stroke(
                couleur,
                style: StrokeStyle(
                    lineWidth: Tokens.Traits.normal * taille / TraceIcone.grille,
                    lineCap: .round,
                    lineJoin: .round
                )
            )
            .frame(width: taille, height: taille)
            .accessibilityHidden(true)
    }
}

struct TraceIcone: Shape {
    static let grille: CGFloat = 24
    let nom: Icone.Nom

    func path(in rect: CGRect) -> Path {
        var p = Path()
        switch nom {
        case .chevronGauche:
            p.ligneBrisee([(15, 6), (9, 12), (15, 18)])
        case .chevronDroit:
            p.ligneBrisee([(9, 6), (15, 12), (9, 18)])
        case .croix:
            p.trait(6, 6, 18, 18)
            p.trait(18, 6, 6, 18)
        case .coche:
            p.ligneBrisee([(5, 12), (10, 17), (19, 7)])
        case .cadenas:
            p.addRoundedRect(in: CGRect(x: 5, y: 11, width: 14, height: 9), cornerSize: CGSize(width: 2, height: 2))
            p.move(to: CGPoint(x: 8, y: 11))
            p.addLine(to: CGPoint(x: 8, y: 8))
            p.demiCercleHaut(centre: CGPoint(x: 12, y: 8), rayon: 4)
            p.addLine(to: CGPoint(x: 16, y: 11))
        case .camera:
            p.addRoundedRect(in: CGRect(x: 3, y: 7, width: 18, height: 13), cornerSize: CGSize(width: 3, height: 3))
            p.addEllipse(in: CGRect(x: 8.5, y: 10, width: 7, height: 7))
            p.ligneBrisee([(9, 7), (10.5, 4), (13.5, 4), (15, 7)])
        case .eclair:
            p.ligneBrisee([(13, 3), (5, 14), (11, 14), (10, 21), (18, 10), (12, 10)])
            p.closeSubpath()
        case .fleche:
            p.trait(5, 12, 19, 12)
            p.ligneBrisee([(13, 6), (19, 12), (13, 18)])
        case .maison:
            p.ligneBrisee([(4, 11), (12, 4), (20, 11)])
            p.ligneBrisee([(6, 10), (6, 19), (18, 19), (18, 10)])
        case .stats:
            p.trait(5, 20, 5, 10)
            p.trait(12, 20, 12, 4)
            p.trait(19, 20, 19, 13)
        case .calendrier:
            p.addRoundedRect(in: CGRect(x: 4, y: 5, width: 16, height: 15), cornerSize: CGSize(width: 2, height: 2))
            p.trait(4, 10, 20, 10)
            p.trait(9, 3, 9, 7)
            p.trait(15, 3, 15, 7)
        case .reglages:
            p.addEllipse(in: CGRect(x: 9, y: 9, width: 6, height: 6))
            p.trait(12, 3, 12, 6)
            p.trait(12, 18, 12, 21)
            p.trait(3, 12, 6, 12)
            p.trait(18, 12, 21, 12)
        }
        let echelle = rect.width / TraceIcone.grille
        return p.applying(CGAffineTransform(scaleX: echelle, y: echelle))
    }
}

/// Symbole de Kova : cadenas ouvert avec un chevron vers le haut. Grille de 104.
struct SymboleKova: View {
    var taille: CGFloat
    var couleur: Color = Tokens.Couleurs.texteSurAccent

    private static let grille: CGFloat = 104

    private struct Corps: Shape {
        func path(in rect: CGRect) -> Path {
            var p = Path()
            p.addRoundedRect(in: CGRect(x: 24, y: 48, width: 56, height: 40), cornerSize: CGSize(width: 10, height: 10))
            p.move(to: CGPoint(x: 36, y: 48))
            p.addLine(to: CGPoint(x: 36, y: 34))
            p.demiCercleHaut(centre: CGPoint(x: 52, y: 34), rayon: 16)
            let echelle = rect.width / SymboleKova.grille
            return p.applying(CGAffineTransform(scaleX: echelle, y: echelle))
        }
    }

    private struct Chevron: Shape {
        func path(in rect: CGRect) -> Path {
            var p = Path()
            p.ligneBrisee([(42, 72), (52, 62), (62, 72)])
            let echelle = rect.width / SymboleKova.grille
            return p.applying(CGAffineTransform(scaleX: echelle, y: echelle))
        }
    }

    var body: some View {
        let echelle = taille / SymboleKova.grille
        ZStack {
            Corps().stroke(couleur, style: StrokeStyle(lineWidth: 10 * echelle, lineCap: .round, lineJoin: .round))
            Chevron().stroke(couleur, style: StrokeStyle(lineWidth: 7 * echelle, lineCap: .round, lineJoin: .round))
        }
        .frame(width: taille, height: taille)
    }
}

/// Logo : le symbole sur un carré arrondi vert citron.
struct LogoKova: View {
    var body: some View {
        SymboleKova(taille: Tokens.Composants.Logo.taille * Tokens.Composants.Logo.proportionSymbole)
            .frame(width: Tokens.Composants.Logo.taille, height: Tokens.Composants.Logo.taille)
            .background(Tokens.Couleurs.accent)
            .clipShape(RoundedRectangle(cornerRadius: Tokens.Composants.Logo.arrondi, style: .continuous))
            .accessibilityElement()
            .accessibilityLabel(Text(verbatim: L.t("a11y.logo")))
    }
}

/// Silhouette en pompe des maquettes. Grille de 150 x 70.
struct FigurePompe: View {
    var animee = true
    @Environment(\.accessibilityReduceMotion) private var reduire
    @State private var enBas = false

    private static let grille = CGSize(width: 150, height: 70)

    private struct Buste: Shape {
        func path(in rect: CGRect) -> Path {
            var p = Path()
            p.addEllipse(in: CGRect(x: 118, y: 10, width: 16, height: 16))
            p.ligneBrisee([(116, 24), (60, 40), (14, 54)])
            return p.applying(FigurePompe.echelle(rect))
        }
    }

    private struct Appuis: Shape {
        func path(in rect: CGRect) -> Path {
            var p = Path()
            p.trait(104, 28, 98, 56)
            p.trait(60, 40, 52, 58)
            return p.applying(FigurePompe.echelle(rect))
        }
    }

    private struct Sol: Shape {
        func path(in rect: CGRect) -> Path {
            var p = Path()
            p.trait(4, 62, 146, 62)
            return p.applying(FigurePompe.echelle(rect))
        }
    }

    fileprivate static func echelle(_ rect: CGRect) -> CGAffineTransform {
        CGAffineTransform(scaleX: rect.width / grille.width, y: rect.height / grille.height)
    }

    var body: some View {
        let largeur = Tokens.Composants.Illustration.largeur
        let hauteur = Tokens.Composants.Illustration.hauteur
        let style = StrokeStyle(lineWidth: Tokens.Traits.illustration, lineCap: .round, lineJoin: .round)
        // La descente vaut 12 unités de la grille.
        let descente = 12 * hauteur / FigurePompe.grille.height
        ZStack {
            Buste().stroke(Tokens.Couleurs.accent, style: style)
                .offset(y: enBas ? descente : 0)
            Appuis().stroke(Tokens.Couleurs.accent, style: style)
            Sol().stroke(Tokens.Couleurs.pointille, style: style)
        }
        .frame(width: largeur, height: hauteur)
        .accessibilityHidden(true)
        .onAppear {
            guard animee, !reduire else { return }
            withAnimation(
                .easeInOut(duration: Double(Tokens.Animations.Pompe.duree) / 2)
                    .repeatForever(autoreverses: true)
            ) {
                enBas = true
            }
        }
    }
}
