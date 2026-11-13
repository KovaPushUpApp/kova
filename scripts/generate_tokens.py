#!/usr/bin/env python3
"""Génère les tokens de design Swift et les couleurs du catalogue d'assets
depuis design-tokens.json. Bibliothèque standard uniquement.

Usage : python3 scripts/generate_tokens.py
        python3 scripts/generate_tokens.py --check   (échoue si les fichiers ne sont pas à jour)
"""
import json
import re
import sys
from pathlib import Path

RACINE = Path(__file__).resolve().parent.parent
SOURCE = RACINE / "design-tokens.json"
SORTIE_SWIFT = RACINE / "Shared" / "Design" / "Tokens.generated.swift"
ASSETS = RACINE / "Kova" / "Resources" / "Assets.xcassets"

# Clés dont la valeur est un entier (compte, graisse, millisecondes), pas une dimension.
CLES_ENTIERES = {"segments", "graisse", "graisses", "intervalleMs", "pas"}
# Couleurs du JSON à exposer aussi dans le catalogue d'assets (écran de lancement, teinte).
COULEURS_ASSETS = {"Fond": "fond", "AccentColor": "accent"}

HEX = re.compile(r"^#[0-9A-Fa-f]{6}$")
IDENT = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def nom_type(cle):
    return cle[0].upper() + cle[1:]


def nombre(valeur):
    if isinstance(valeur, int):
        return str(valeur)
    return repr(float(valeur))


def chaine(valeur):
    return json.dumps(valeur, ensure_ascii=False)


def verifier_ident(cle):
    if not IDENT.match(cle):
        raise SystemExit(f"Clé de token invalide pour Swift : {cle!r}")


def emettre(cle, valeur, niveau, lignes, uikit):
    """Écrit la déclaration Swift de `cle`. `uikit` : couleurs en UIColor plutôt qu'en Color."""
    verifier_ident(cle)
    pad = "    " * niveau
    if isinstance(valeur, dict):
        lignes.append(f"{pad}enum {nom_type(cle)} {{")
        for k, v in valeur.items():
            emettre(k, v, niveau + 1, lignes, uikit)
        lignes.append(f"{pad}}}")
    elif isinstance(valeur, bool):
        lignes.append(f"{pad}static let {cle} = {'true' if valeur else 'false'}")
    elif isinstance(valeur, (int, float)):
        if cle in CLES_ENTIERES:
            lignes.append(f"{pad}static let {cle}: Int = {int(valeur)}")
        else:
            lignes.append(f"{pad}static let {cle}: CGFloat = {nombre(valeur)}")
    elif isinstance(valeur, str):
        if HEX.match(valeur):
            type_couleur = "UIColor" if uikit else "Color"
            lignes.append(f"{pad}static let {cle} = {type_couleur}(hex: 0x{valeur[1:].upper()})")
        else:
            lignes.append(f"{pad}static let {cle} = {chaine(valeur)}")
    elif isinstance(valeur, list):
        if all(isinstance(x, (int, float)) and not isinstance(x, bool) for x in valeur):
            if cle in CLES_ENTIERES:
                contenu = ", ".join(str(int(x)) for x in valeur)
                lignes.append(f"{pad}static let {cle}: [Int] = [{contenu}]")
            else:
                contenu = ", ".join(nombre(x) for x in valeur)
                lignes.append(f"{pad}static let {cle}: [CGFloat] = [{contenu}]")
        else:
            raise SystemExit(f"Liste non prise en charge pour le token {cle!r}")
    else:
        raise SystemExit(f"Type non pris en charge pour le token {cle!r}")


def generer_swift(tokens):
    l = [
        "// Fichier généré par scripts/generate_tokens.py depuis design-tokens.json.",
        "// Ne pas modifier à la main : changer le JSON puis relancer le script.",
        "",
        "import SwiftUI",
        "import UIKit",
        "",
        "enum Tokens {",
    ]
    for cle, valeur in tokens.items():
        if cle == "espacements":
            l.append("    /// Suite de Fibonacci. `e21` vaut 21 points.")
            l.append("    enum Espacements {")
            for x in valeur:
                l.append(f"        static let e{int(x)}: CGFloat = {nombre(x)}")
            l.append(f"        static let tous: [CGFloat] = [{', '.join(nombre(x) for x in valeur)}]")
            l.append("    }")
        elif cle == "couleurs":
            emettre("couleurs", valeur, 1, l, uikit=False)
            l.append("    /// Mêmes couleurs en UIColor, pour l'extension d'écran de verrou.")
            emettre("couleursUIKit", valeur, 1, l, uikit=True)
        else:
            emettre(cle, valeur, 1, l, uikit=False)
    l += [
        "}",
        "",
        "extension Color {",
        "    init(hex: UInt32) {",
        "        self.init(",
        "            .sRGB,",
        "            red: Double((hex >> 16) & 0xFF) / 255,",
        "            green: Double((hex >> 8) & 0xFF) / 255,",
        "            blue: Double(hex & 0xFF) / 255,",
        "            opacity: 1",
        "        )",
        "    }",
        "}",
        "",
        "extension UIColor {",
        "    convenience init(hex: UInt32) {",
        "        self.init(",
        "            red: CGFloat((hex >> 16) & 0xFF) / 255,",
        "            green: CGFloat((hex >> 8) & 0xFF) / 255,",
        "            blue: CGFloat(hex & 0xFF) / 255,",
        "            alpha: 1",
        "        )",
        "    }",
        "}",
        "",
    ]
    return "\n".join(l)


def generer_colorset(hexa):
    r, g, b = hexa[1:3], hexa[3:5], hexa[5:7]
    contenu = {
        "colors": [
            {
                "idiom": "universal",
                "color": {
                    "color-space": "srgb",
                    "components": {
                        "red": f"0x{r.upper()}",
                        "green": f"0x{g.upper()}",
                        "blue": f"0x{b.upper()}",
                        "alpha": "1.000",
                    },
                },
            }
        ],
        "info": {"author": "xcode", "version": 1},
    }
    return json.dumps(contenu, indent=2) + "\n"


def main():
    verifier = "--check" in sys.argv
    tokens = json.loads(SOURCE.read_text(encoding="utf-8"))

    fichiers = {SORTIE_SWIFT: generer_swift(tokens)}
    for nom_asset, cle in COULEURS_ASSETS.items():
        fichiers[ASSETS / f"{nom_asset}.colorset" / "Contents.json"] = generer_colorset(tokens["couleurs"][cle])

    perimes = []
    for chemin, contenu in fichiers.items():
        actuel = chemin.read_text(encoding="utf-8") if chemin.exists() else None
        if actuel == contenu:
            continue
        if verifier:
            perimes.append(chemin)
        else:
            chemin.parent.mkdir(parents=True, exist_ok=True)
            chemin.write_text(contenu, encoding="utf-8", newline="\n")
            print(f"écrit : {chemin.relative_to(RACINE)}")

    if verifier and perimes:
        for chemin in perimes:
            print(f"pas à jour : {chemin.relative_to(RACINE)}")
        raise SystemExit(1)
    print("tokens à jour")


if __name__ == "__main__":
    main()
