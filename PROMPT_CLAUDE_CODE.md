# Prompt à coller dans Claude Code

Ouvre Claude Code dans le dossier `Kova App`, puis colle le bloc ci-dessous.

```
Tu vas construire Kova, une app iOS. Tout le cahier des charges est dans ce dossier.

Commence par lire, dans cet ordre : CLAUDE.md, BRIEF.md, design-tokens.json, puis les 24 fichiers de maquettes/ (styles en ligne = valeurs exactes, script du bas = logique d'état).

Ensuite, avant d'écrire du code :
1. Résume-moi le projet en 10 lignes pour que je vérifie que tu as compris.
2. Vérifie dans la documentation Apple à jour chaque point de la section 9 du brief (Family Controls, sélecteur d'apps, écran de verrou, durée minimale des intervalles, simulateur). Dis-moi ce qui est confirmé, ce qui diffère, et ce que ça change.
3. Propose le plan de construction en étapes vérifiables, et liste les actions que je dois faire moi-même (section 12 du brief) avec le moment où chacune devient bloquante.
Attends mon accord avant de coder.

Ordre de construction attendu :
Étape 1. Socle : project.yml (XcodeGen), les 4 cibles et l'App Group, tokens de design en Swift, polices, localisation FR et EN, navigation, configuration de la CI.
Étape 2. Tous les écrans en SwiftUI avec données fictives, fidèles aux maquettes, animations comprises.
Étape 3. Comptage des pompes avec Vision, puis l'écran d'essai de l'onboarding. Ensuite les squats.
Étape 4. Verrouillage réel : sélection des apps, écran de verrou, fenêtre de déblocage, reverrouillage automatique.
Étape 5. Règles du jeu : solde, règle réglable, urgence, planning, délai de 24 h, série, stats.
Étape 6. Abonnements avec RevenueCat : paywall, essai, offre de rattrapage, restauration, version limitée.
Étape 7. Analytics, accessibilité, textes anglais, finitions.
Étape 8. Préparation App Store : icône, captures, fiche (titre, sous-titre, mots-clés du brief), conditions et confidentialité, liste de contrôle avant soumission.

Règles :
- Je suis sous Windows, sans Mac. Tout build passe par la CI et je teste sur mon iPhone via TestFlight.
- À la fin de chaque étape : ce qui est fait, comment je le vérifie sur mon iPhone, ce qui attend une action de ma part.
- Ne valide jamais une étape sur la base d'une supposition : si tu ne peux pas tester, dis-le.
- Reprends les critères d'acceptation de la section 11 du brief comme liste de contrôle finale.
```

## Avant de lancer

Trois actions à faire toi-même le plus tôt possible, car elles ont un délai :
1. Créer la fiche Kova dans App Store Connect.
2. Demander l'autorisation Family Controls à Apple.
3. Trancher le point ouvert n°1 du brief (précommande à vie) avant de vendre quoi que ce soit.
