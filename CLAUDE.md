# Kova

App iOS qui verrouille les apps distrayantes et les débloque contre des pompes comptées par la caméra.

## À lire avant tout

1. `BRIEF.md` : le cahier des charges complet (concept, règles, écrans, technique, critères d'acceptation).
2. `design-tokens.json` : toutes les valeurs de design.
3. `maquettes/` : un fichier `.dc.html` par écran. Lire les styles en ligne pour les valeurs exactes et le `<script>` du bas pour la logique. `{{accent}}` vaut `#C8F53C`.

## Règles de travail

- Le brief fait foi. En cas de doute entre la maquette et la section 9 du brief (contraintes Apple), la section 9 gagne.
- Swift, SwiftUI, iOS 16 minimum. Projet décrit avec XcodeGen (`project.yml`). Le poste de développement est sous Windows : ne jamais supposer qu'Xcode est disponible en local, tout build passe par la CI.
- Aucune valeur de design en dur dans les vues : passer par un fichier de tokens Swift généré depuis `design-tokens.json`.
- Tous les textes dans des fichiers de localisation, français et anglais, dès le premier écran.
- Textes rédigés en français : tutoiement, phrases courtes, pas de tiret cadratin.
- L'image de la caméra ne doit jamais être enregistrée ni envoyée. Aucune exception.
- Avancer par petites étapes vérifiables, dans l'ordre de `PROMPT_CLAUDE_CODE.md`. Après chaque étape, dire ce qui est fait, ce qui est testé et ce qui attend une action humaine.
- Ne jamais inventer un comportement de l'API Temps d'écran : vérifier dans la documentation Apple, et signaler tout écart avec le brief.
- Les étapes de la section 12 du brief sont humaines. Les lister clairement quand elles deviennent bloquantes, sans les simuler.
