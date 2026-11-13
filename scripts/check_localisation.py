#!/usr/bin/env python3
"""Contrôle les fichiers de localisation. Bibliothèque standard uniquement.

Pour chaque dossier qui contient des .lproj :
  - le français et l'anglais ont exactement les mêmes clés, sans doublon ;
  - aucune valeur n'est vide ;
  - les textes français n'ont pas de tiret cadratin ;
  - les valeurs insérées (%lld, %@) sont les mêmes, dans le même ordre ;
  - chaque clé citée dans le code Swift du dossier existe ;
  - chaque clé des fichiers est utilisée par le code.

Usage : python3 scripts/check_localisation.py
"""
import re
import sys
from pathlib import Path

RACINE = Path(__file__).resolve().parent.parent
LANGUES = ["fr", "en"]
# Dossier des .lproj -> dossiers Swift qui utilisent ces textes.
ENSEMBLES = {
    "Kova/Resources": ["Kova"],
    "KovaShieldConfig": ["KovaShieldConfig"],
}

LIGNE = re.compile(r'^"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$')
FORMAT = re.compile(r"%(?:lld|@)")
LITTERAL = re.compile(r'"([a-z][a-z0-9_]*(?:\.[a-z0-9_]+)+)"')


def lire(chemin, erreurs):
    cles = {}
    dans_commentaire = False
    for numero, brute in enumerate(chemin.read_text(encoding="utf-8").splitlines(), 1):
        ligne = brute.strip()
        if dans_commentaire:
            if "*/" in ligne:
                dans_commentaire = False
            continue
        if not ligne or ligne.startswith("//"):
            continue
        if ligne.startswith("/*"):
            if "*/" not in ligne:
                dans_commentaire = True
            continue
        m = LIGNE.match(ligne)
        if not m:
            erreurs.append(f"{chemin.relative_to(RACINE)}:{numero} : ligne illisible")
            continue
        cle, valeur = m.group(1), m.group(2)
        if cle in cles:
            erreurs.append(f"{chemin.relative_to(RACINE)}:{numero} : clé en double {cle}")
        if not valeur.strip():
            erreurs.append(f"{chemin.relative_to(RACINE)}:{numero} : valeur vide pour {cle}")
        cles[cle] = valeur
    return cles


def main():
    erreurs = []
    total = 0
    for dossier, sources in ENSEMBLES.items():
        base = RACINE / dossier
        noms = sorted({f.name for f in base.glob("*.lproj/*.strings")})
        toutes_les_cles = set()
        for nom in noms:
            tables = {}
            for langue in LANGUES:
                chemin = base / f"{langue}.lproj" / nom
                if not chemin.exists():
                    erreurs.append(f"{dossier}/{langue}.lproj/{nom} : fichier manquant")
                    tables[langue] = {}
                else:
                    tables[langue] = lire(chemin, erreurs)
            reference = set(tables[LANGUES[0]])
            for langue in LANGUES[1:]:
                for cle in sorted(reference - set(tables[langue])):
                    erreurs.append(f"{dossier}/{nom} : {cle} manque en {langue}")
                for cle in sorted(set(tables[langue]) - reference):
                    erreurs.append(f"{dossier}/{nom} : {cle} manque en {LANGUES[0]}")
            for cle in sorted(reference & set(tables["en"])):
                if FORMAT.findall(tables["fr"][cle]) != FORMAT.findall(tables["en"][cle]):
                    erreurs.append(f"{dossier}/{nom} : valeurs %lld ou %@ différentes entre fr et en pour {cle}")
            for cle, valeur in tables["fr"].items():
                if "—" in valeur:
                    erreurs.append(f"{dossier}/{nom} : tiret cadratin dans {cle}")
            if nom == "Localizable.strings":
                toutes_les_cles |= reference
            total += len(reference)

        # Une chaîne Swift qui commence par un préfixe de clé connu doit exister.
        prefixes = {cle.split(".")[0] for cle in toutes_les_cles}
        utilisees = set()
        for source in sources:
            for swift in sorted((RACINE / source).rglob("*.swift")):
                for numero, ligne in enumerate(swift.read_text(encoding="utf-8").splitlines(), 1):
                    for cle in LITTERAL.findall(ligne):
                        if cle.split(".")[0] not in prefixes:
                            continue
                        utilisees.add(cle)
                        accordee = cle + ".un" in toutes_les_cles and cle + ".autre" in toutes_les_cles
                        if cle not in toutes_les_cles and not accordee:
                            erreurs.append(f"{swift.relative_to(RACINE)}:{numero} : clé inconnue {cle}")
        for cle in sorted(toutes_les_cles):
            base = cle.rsplit(".", 1)[0] if cle.endswith((".un", ".autre")) else cle
            if cle not in utilisees and base not in utilisees:
                erreurs.append(f"{dossier} : clé jamais utilisée {cle}")

    if erreurs:
        print("\n".join(erreurs))
        sys.exit(1)
    print(f"localisation correcte : {total} clés, langues {', '.join(LANGUES)}")


if __name__ == "__main__":
    main()
