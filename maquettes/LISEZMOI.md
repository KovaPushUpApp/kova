# Maquettes Kova

Un fichier par écran, au format 390 x 844 px (sauf l'icône, 240 x 240).

Comment les lire :
- Les styles en ligne (`style="..."`) donnent les valeurs exactes : tailles, couleurs, espacements, arrondis.
- `{{accent}}` vaut `#C8F53C`.
- Les `<a href="X.dc.html">` indiquent vers quel écran mène chaque bouton.
- Le bloc `<script type="text/x-dc">` en bas de fichier décrit la logique d'état de l'écran (sélection, compteurs, minuteries).
- Les animations sont nommées dans le `<style>` du haut (up, pop, pulse, grow, fill, ring, scan, pushup).
- `<sc-for>` répète un bloc pour chaque élément d'une liste, `<sc-if>` affiche un bloc sous condition.

Ces fichiers ne s'ouvrent pas tels quels dans un navigateur (ils dépendent de l'éditeur de maquettes). Ils servent de référence de lecture.

Ordre du parcours :
1. Onboarding : Bienvenue, Temps, Choc, Objectif, Apps, Regle, AutoEcran, AutoCamera, Essai, Paywall, Offre
2. Coeur : Main, Verrou, Exercice, Placement, Compteur, Gagne, Urgence
3. Onglets : Stats, Planning, Reglages, AppsReglage, Abonnement
4. Icone

Les écarts volontaires entre maquettes et app réelle sont listés dans BRIEF.md, section 9.
