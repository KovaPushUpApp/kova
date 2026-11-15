// Fichier généré par scripts/generate_tokens.py depuis design-tokens.json.
// Ne pas modifier à la main : changer le JSON puis relancer le script.

import SwiftUI
import UIKit

enum Tokens {
    static let nom = "Kova"
    static let principe = "Nombre d'or (1,618) pour les tailles de texte et les proportions, suite de Fibonacci pour les espacements"
    enum Ecran {
        static let largeur: CGFloat = 390
        static let hauteur: CGFloat = 844
        static let margeHaut: CGFloat = 55
        static let margeCotes: CGFloat = 21
        static let margeBas: CGFloat = 34
    }
    enum Couleurs {
        static let fond = Color(hex: 0x0E0E10)
        static let carte = Color(hex: 0x1A1A1E)
        static let carteSelectionnee = Color(hex: 0x23261A)
        static let ligne = Color(hex: 0x2C2C33)
        static let ligneFine = Color(hex: 0x26262B)
        static let pointille = Color(hex: 0x3A3A42)
        static let texte = Color(hex: 0xF4F4F1)
        static let texteSecondaire = Color(hex: 0xA3A3AB)
        static let accent = Color(hex: 0xC8F53C)
        static let texteSurAccent = Color(hex: 0x0E0E10)
        static let barreInactive = Color(hex: 0x4A4D3A)
    }
    /// Mêmes couleurs en UIColor, pour l'extension d'écran de verrou.
    enum CouleursUIKit {
        static let fond = UIColor(hex: 0x0E0E10)
        static let carte = UIColor(hex: 0x1A1A1E)
        static let carteSelectionnee = UIColor(hex: 0x23261A)
        static let ligne = UIColor(hex: 0x2C2C33)
        static let ligneFine = UIColor(hex: 0x26262B)
        static let pointille = UIColor(hex: 0x3A3A42)
        static let texte = UIColor(hex: 0xF4F4F1)
        static let texteSecondaire = UIColor(hex: 0xA3A3AB)
        static let accent = UIColor(hex: 0xC8F53C)
        static let texteSurAccent = UIColor(hex: 0x0E0E10)
        static let barreInactive = UIColor(hex: 0x4A4D3A)
    }
    enum Polices {
        enum Titres {
            static let famille = "Space Grotesk"
            static let graisse: Int = 700
        }
        enum Texte {
            static let famille = "Inter"
            static let graisses: [Int] = [400, 500, 600]
        }
    }
    enum TaillesTexte {
        static let legende: CGFloat = 13
        static let corps: CGFloat = 16
        static let corpsFort: CGFloat = 20
        static let titreCarte: CGFloat = 26
        static let titreEcran: CGFloat = 33
        static let titreAccueil: CGFloat = 42
        static let chiffre: CGFloat = 68
        static let chiffreGeant: CGFloat = 110
        static let chiffreChoc: CGFloat = 178
    }
    enum Interligne {
        static let paragraphe: CGFloat = 1.618
        static let titre: CGFloat = 1.12
        static let chiffre: CGFloat = 1.0
    }
    /// Suite de Fibonacci. `e21` vaut 21 points.
    enum Espacements {
        static let e3: CGFloat = 3
        static let e5: CGFloat = 5
        static let e8: CGFloat = 8
        static let e13: CGFloat = 13
        static let e21: CGFloat = 21
        static let e34: CGFloat = 34
        static let e55: CGFloat = 55
        static let e89: CGFloat = 89
        static let e144: CGFloat = 144
        static let tous: [CGFloat] = [3, 5, 8, 13, 21, 34, 55, 89, 144]
    }
    enum Arrondis {
        static let petit: CGFloat = 8
        static let moyen: CGFloat = 13
        static let grand: CGFloat = 21
        static let tresGrand: CGFloat = 34
        static let rond = "50%"
    }
    enum Hauteurs {
        static let boutonPrincipal: CGFloat = 55
        static let option: CGFloat = 55
        static let onglet: CGFloat = 55
        static let boutonSecondaire: CGFloat = 44
        static let boutonIcone: CGFloat = 44
        static let ligneReglage: CGFloat = 55
    }
    enum Composants {
        enum Interrupteur {
            static let largeur: CGFloat = 55
            static let hauteur: CGFloat = 34
            static let pastille: CGFloat = 28
            static let decalageActif: CGFloat = 21
        }
        enum VignetteApp {
            static let taille: CGFloat = 34
            static let arrondi: CGFloat = 8
        }
        enum Logo {
            static let taille: CGFloat = 89
            static let arrondi: CGFloat = 21
            static let proportionSymbole: CGFloat = 0.618
        }
        enum VueCamera {
            static let hauteurSeance: CGFloat = 322
            static let hauteurEssai: CGFloat = 233
            static let partDeLecran: CGFloat = 0.382
        }
        enum BarreProgression {
            static let segments: Int = 10
            static let hauteur: CGFloat = 8
            static let ecart: CGFloat = 5
        }
        enum Illustration {
            static let largeur: CGFloat = 170
            static let hauteur: CGFloat = 80
        }
        enum Graphique {
            static let hauteur: CGFloat = 233
            static let barreMax: CGFloat = 144
            static let barreVide: CGFloat = 4
        }
        enum BarreAnnee {
            static let hauteur: CGFloat = 13
            static let arrondi: CGFloat = 8
        }
    }
    enum Animations {
        static let dureeStandard: CGFloat = 0.5
        static let courbe = "ease"
        enum Entree {
            static let nom = "up"
            static let opacite: [CGFloat] = [0, 1]
            static let translationY: [CGFloat] = [18, 0]
            static let cascade: [CGFloat] = [0.07, 0.15]
        }
        enum Pop {
            static let duree: CGFloat = 0.6
            static let echelle: [CGFloat] = [0.6, 1.06, 1.0]
            static let pic: CGFloat = 0.7
        }
        enum Pulsation {
            static let duree: CGFloat = 2.4
            static let echelle: [CGFloat] = [1.0, 1.035, 1.0]
            static let boucle = true
        }
        enum CroissanceBarre {
            static let duree: CGFloat = 0.6
            static let origine = "bas"
        }
        enum Remplissage {
            static let duree: CGFloat = 0.6
            static let origine = "gauche"
        }
        enum Anneau {
            static let duree: CGFloat = 2.0
            static let echelle: [CGFloat] = [0.7, 1.9]
            static let opacite: [CGFloat] = [0.8, 0]
            static let boucle = true
        }
        enum ScanCamera {
            static let duree: CGFloat = 3.0
            static let boucle = true
            static let debut: CGFloat = 0.08
            static let fin: CGFloat = 0.88
        }
        enum CompteurChoc {
            static let pas: Int = 2
            static let intervalleMs: Int = 28
        }
        enum CompteurGain {
            static let pas: Int = 1
            static let intervalleMs: Int = 55
        }
        static let reduireLesAnimations = "respecter le réglage système"
        enum Pompe {
            static let duree: CGFloat = 1.6
            static let boucle = true
        }
    }
    enum Haptique {
        static let repetition = "léger"
        static let serieValidee = "fort"
    }
    enum Traits {
        static let fin: CGFloat = 1
        static let normal: CGFloat = 2
        static let illustration: CGFloat = 3
    }
    enum Opacites {
        static let voileSurAccent: CGFloat = 0.12
        static let scan: CGFloat = 0.5
        static let voileCamera: CGFloat = 0.85
    }
    enum Typographie {
        static let echelleMinimale: CGFloat = 0.5
        static let interlettrageSurtitre: CGFloat = 0.08
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}
