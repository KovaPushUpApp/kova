#!/usr/bin/env python3
"""Génère pour Android, depuis les sources communes du projet :
  - les tokens de design en Kotlin, depuis design-tokens.json ;
  - les textes français et anglais (strings.xml), depuis les fichiers
    Localizable.strings de l'app iPhone, complétés ou remplacés par
    android/textes/*.strings pour ce qui est propre à Android ;
  - les polices, copiées depuis Kova/Resources/Fonts.

Bibliothèque standard uniquement.
Usage : python android/outils/generer.py          (écrit les fichiers)
        python android/outils/generer.py --check  (échoue s'ils ne sont pas à jour)
"""
import json
import re
import shutil
import sys
from pathlib import Path

ANDROID = Path(__file__).resolve().parent.parent
RACINE = ANDROID.parent
RES = ANDROID / "app" / "src" / "main" / "res"
KOTLIN = ANDROID / "app" / "src" / "main" / "java" / "com" / "getkova" / "app"

CLES_ENTIERES = {"segments", "graisse", "graisses", "intervalleMs", "pas"}
HEX = re.compile(r"^#[0-9A-Fa-f]{6}$")
LIGNE = re.compile(r'^"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$')
FORMAT = re.compile(r"%(0\d+)?(lld|@)")

POLICES = {
    "SpaceGrotesk-Bold.ttf": "space_grotesk_bold.ttf",
    "Inter-Regular.ttf": "inter_regular.ttf",
    "Inter-Medium.ttf": "inter_medium.ttf",
    "Inter-SemiBold.ttf": "inter_semibold.ttf",
}


# --- Tokens -----------------------------------------------------------------

def nombre(v):
    return f"{float(v)}f"


def emettre(cle, valeur, niveau, lignes):
    pad = "    " * niveau
    if isinstance(valeur, dict):
        lignes.append(f"{pad}object {cle[0].upper() + cle[1:]} {{")
        for k, v in valeur.items():
            emettre(k, v, niveau + 1, lignes)
        lignes.append(f"{pad}}}")
    elif isinstance(valeur, bool):
        lignes.append(f"{pad}const val {cle} = {'true' if valeur else 'false'}")
    elif isinstance(valeur, (int, float)):
        if cle in CLES_ENTIERES:
            lignes.append(f"{pad}const val {cle} = {int(valeur)}")
        else:
            lignes.append(f"{pad}const val {cle} = {nombre(valeur)}")
    elif isinstance(valeur, str):
        if HEX.match(valeur):
            lignes.append(f"{pad}val {cle} = Color(0xFF{valeur[1:].upper()})")
        else:
            lignes.append(f"{pad}const val {cle} = {json.dumps(valeur, ensure_ascii=False)}")
    elif isinstance(valeur, list):
        if cle in CLES_ENTIERES:
            lignes.append(f"{pad}val {cle} = listOf({', '.join(str(int(x)) for x in valeur)})")
        else:
            lignes.append(f"{pad}val {cle} = listOf({', '.join(nombre(x) for x in valeur)})")
    else:
        raise SystemExit(f"Type non pris en charge pour le token {cle!r}")


def tokens_kotlin():
    tokens = json.loads((RACINE / "design-tokens.json").read_text(encoding="utf-8"))
    l = [
        "// Fichier généré par android/outils/generer.py depuis design-tokens.json.",
        "// Ne pas modifier à la main : changer le JSON puis relancer le script.",
        "// Les dimensions sont des nombres : ajouter .dp ou .sp à l'usage.",
        "",
        "package com.getkova.app.design",
        "",
        "import androidx.compose.ui.graphics.Color",
        "",
        "object Tokens {",
    ]
    for cle, valeur in tokens.items():
        if cle == "espacements":
            l.append("    /** Suite de Fibonacci. `e21` vaut 21. */")
            l.append("    object Espacements {")
            for x in valeur:
                l.append(f"        const val e{int(x)} = {nombre(x)}")
            l.append("    }")
        else:
            emettre(cle, valeur, 1, l)
    l += ["}", ""]
    return "\n".join(l), tokens


# --- Textes -----------------------------------------------------------------

def lire_strings(chemin):
    cles = {}
    if not chemin.exists():
        return cles
    for ligne in chemin.read_text(encoding="utf-8").splitlines():
        m = LIGNE.match(ligne.strip())
        if m:
            cles[m.group(1)] = m.group(2)
    return cles


def vers_android(valeur):
    """Convertit un texte au format iOS vers le format des ressources Android."""
    texte = valeur.replace('\\"', '"').replace("\\\\", "\\")
    rang = [0]

    def remplacer(m):
        rang[0] += 1
        largeur = m.group(1) or ""
        return f"%{rang[0]}${largeur}{'d' if m.group(2) == 'lld' else 's'}"

    texte = FORMAT.sub(remplacer, texte)
    texte = texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    texte = texte.replace("'", "\\'").replace('"', '\\"')
    if texte[:1] in ("@", "?"):
        texte = "\\" + texte
    return texte


def strings_xml(langue):
    cles = lire_strings(RACINE / "Kova" / "Resources" / f"{langue}.lproj" / "Localizable.strings")
    cles.update(lire_strings(ANDROID / "textes" / f"{langue}.strings"))
    l = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<!-- Fichier généré par android/outils/generer.py. Ne pas modifier à la main. -->",
        "<resources>",
        '    <string name="nom_app">Kova</string>',
    ]
    for cle in sorted(cles):
        nom = cle.replace(".", "_")
        l.append(f'    <string name="{nom}">{vers_android(cles[cle])}</string>')
    l += ["</resources>", ""]
    return "\n".join(l), set(cles)


def couleurs_xml(tokens):
    l = ['<?xml version="1.0" encoding="utf-8"?>',
         "<!-- Fichier généré par android/outils/generer.py. Ne pas modifier à la main. -->",
         "<resources>"]
    for cle in ("fond", "accent"):
        l.append(f'    <color name="{cle}">{tokens["couleurs"][cle].upper()}</color>')
    l += ["</resources>", ""]
    return "\n".join(l)


def main():
    verifier = "--check" in sys.argv
    kotlin, tokens = tokens_kotlin()
    fr, cles_fr = strings_xml("fr")
    en, cles_en = strings_xml("en")
    if cles_fr != cles_en:
        ecart = sorted(cles_fr ^ cles_en)
        raise SystemExit(f"Clés différentes entre fr et en : {ecart}")

    fichiers = {
        KOTLIN / "design" / "Tokens.kt": kotlin,
        # Le français est la langue par défaut de l'app.
        RES / "values" / "strings.xml": fr,
        RES / "values-en" / "strings.xml": en,
        RES / "values" / "colors.xml": couleurs_xml(tokens),
    }
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

    for source, cible in POLICES.items():
        origine = RACINE / "Kova" / "Resources" / "Fonts" / source
        destination = RES / "font" / cible
        if not destination.exists() or destination.read_bytes() != origine.read_bytes():
            if verifier:
                perimes.append(destination)
            else:
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(origine, destination)
                print(f"copié : {destination.relative_to(RACINE)}")

    if verifier and perimes:
        for chemin in perimes:
            print(f"pas à jour : {chemin.relative_to(RACINE)}")
        raise SystemExit(1)
    print(f"Android à jour : {len(cles_fr)} textes par langue")


if __name__ == "__main__":
    main()
