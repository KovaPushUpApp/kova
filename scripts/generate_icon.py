#!/usr/bin/env python3
"""Dessine l'icône de l'app (1024 x 1024, sans transparence) d'après le tracé de
maquettes/Icone.dc.html et les couleurs de design-tokens.json.

Demande Pillow (pip install pillow). À relancer seulement si le logo change :
le PNG produit est versionné, la CI n'exécute pas ce script.
"""
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

RACINE = Path(__file__).resolve().parent.parent
SORTIE = RACINE / "Kova" / "Resources" / "Assets.xcassets" / "AppIcon.appiconset" / "icone-1024.png"

TAILLE = 1024
SUR = 4  # suréchantillonnage pour l'anticrénelage
VIEWBOX = 104.0
PART_SYMBOLE = 111.0 / 180.0  # taille du symbole dans la maquette de l'icône


def main():
    tokens = json.loads((RACINE / "design-tokens.json").read_text(encoding="utf-8"))
    fond = tokens["couleurs"]["accent"]
    trait = tokens["couleurs"]["texteSurAccent"]

    cote = TAILLE * SUR
    echelle = cote * PART_SYMBOLE / VIEWBOX
    decalage = (cote - VIEWBOX * echelle) / 2

    image = Image.new("RGB", (cote, cote), fond)
    dessin = ImageDraw.Draw(image)

    def p(x, y):
        return (decalage + x * echelle, decalage + y * echelle)

    def tampon(x, y, epaisseur):
        cx, cy = p(x, y)
        r = epaisseur * echelle / 2
        dessin.ellipse((cx - r, cy - r, cx + r, cy + r), fill=trait)

    def segment(a, b, epaisseur):
        pas = max(2, int(math.hypot(b[0] - a[0], b[1] - a[1]) * echelle / 2))
        for i in range(pas + 1):
            t = i / pas
            tampon(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, epaisseur)

    # Corps du cadenas : rect x=24 y=48 w=56 h=40 rx=10, trait de 10.
    e = 10
    x0, y0 = p(24 - e / 2, 48 - e / 2)
    x1, y1 = p(80 + e / 2, 88 + e / 2)
    dessin.rounded_rectangle((x0, y0, x1, y1), radius=(10 + e / 2) * echelle, fill=trait)
    x0, y0 = p(24 + e / 2, 48 + e / 2)
    x1, y1 = p(80 - e / 2, 88 - e / 2)
    dessin.rounded_rectangle((x0, y0, x1, y1), radius=(10 - e / 2) * echelle, fill=fond)

    # Anse ouverte : M36 48 V34 a16 16 0 0 1 32 0, trait de 10.
    segment((36, 48), (36, 34), 10)
    pas = 400
    for i in range(pas + 1):
        angle = math.pi + math.pi * i / pas
        tampon(52 + 16 * math.cos(angle), 34 + 16 * math.sin(angle), 10)

    # Chevron : M42 72 L52 62 L62 72, trait de 7.
    segment((42, 72), (52, 62), 7)
    segment((52, 62), (62, 72), 7)

    final = image.resize((TAILLE, TAILLE), Image.LANCZOS)
    SORTIE.parent.mkdir(parents=True, exist_ok=True)
    final.save(SORTIE, format="PNG")
    print(f"écrit : {SORTIE.relative_to(RACINE)}")


if __name__ == "__main__":
    main()
