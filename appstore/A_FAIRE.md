# Ce que seul Julien peut faire

Liste des actions humaines (brief, section 12), dans l'ordre où elles bloquent.
Rien de ce qui suit n'est simulé dans le code.

## Pour savoir si le code compile (gratuit, sans compte Apple)

1. Créer un dépôt vide sur GitHub et y pousser le projet.
2. L'onglet "Actions" lance tout seul `Compilation et tests` : compilation de l'app
   et des 3 extensions, puis tests des règles du jeu et du comptage.
   Dépôt public : gratuit. Dépôt privé : les minutes macOS comptent 10 fois.

C'est la première chose à faire : aucune ligne de Swift n'a encore été compilée.

## Pour installer l'app sur un iPhone

| # | Action | Où | Bloque |
|---|---|---|---|
| 1 | Compte Apple Developer actif | developer.apple.com | Tout build signé |
| 2 | Enregistrer les 4 identifiants et l'App Group (liste en tête de `project.yml`) | Certificates, Identifiers & Profiles | Tout build signé |
| 3 | Demander Family Controls (Distribution) pour les 4 identifiants | developer.apple.com/contact/request/family-controls-distribution | Le verrouillage réel sur TestFlight, et la mise en ligne. Délai : de 1 jour à plus d'un mois |
| 4 | Créer la fiche Kova | App Store Connect | TestFlight |
| 5 | Créer la clé API App Store Connect et la donner à Codemagic sous le nom `codemagic` | App Store Connect, puis Codemagic | TestFlight |
| 6 | Créer la variable `CERTIFICATE_PRIVATE_KEY` (groupe `signature`) | Codemagic | TestFlight |
| 7 | Avoir un iPhone sous iOS 16 ou plus | | Tous les tests réels |

## Pour les abonnements

| # | Action | Détail |
|---|---|---|
| 8 | Créer un groupe d'abonnements et 3 produits dans App Store Connect | Annuel à 29,99 € avec essai gratuit de 3 jours. Hebdomadaire à 4,99 €. Annuel "rattrapage" à 29,99 € avec offre de lancement payée d'avance à 19,99 € la première année |
| 9 | Créer le projet RevenueCat | Un droit nommé `premium`. Offre courante : forfaits annuel et hebdomadaire. Offre nommée `rattrapage` : le forfait annuel de rattrapage |
| 10 | Copier la clé publique iOS de RevenueCat dans `KOVA_REVENUECAT_CLE` (`project.base.yml`) | Sans elle, les achats sont indisponibles dans l'app |
| 11 | Trancher la précommande "fondateur à vie" (brief, point ouvert 1) | Un code saisi dans l'app est interdit par Apple. Piste : un achat "à vie" dans l'app, offert aux fondateurs par codes d'offre Apple. À décider avant de vendre |

## Pour la mise en ligne

| # | Action | Détail |
|---|---|---|
| 12 | Faire valider `confidentialite.md` et `conditions.md`, les publier, mettre leurs adresses dans `project.base.yml` | Brouillons à relire par un juriste. Le lien de confidentialité est obligatoire |
| 13 | Renseigner `KOVA_CONTACT` et, si voulu, `KOVA_TELEMETRYDECK_ID` | `project.base.yml` |
| 14 | Remplir l'étiquette de confidentialité | Réponses prêtes dans `etiquette-confidentialite.md` |
| 15 | Faire les captures d'écran sur un iPhone ou via la CI | Tailles dans `LISTE_DE_CONTROLE.md` |
| 16 | Vérifier la marque Kova à l'INPI | Brief, point ouvert 4 |
| 17 | Retirer l'écran "Contrôle du socle" | Me le demander : il sert tant que l'app n'est pas testée |

## Points à décider (brief, section 13)

- **Mots-clés** : la liste du brief fait 100 caractères mais 103 octets à cause des accents.
  Si App Store Connect la refuse, retirer `digital`.
- **Version gratuite** : 1 app et pompes seules, comme proposé. À tester face à un abonnement obligatoire.
- **Ordre de l'onboarding** : l'autorisation Temps d'écran passe avant le choix des apps,
  car le sélecteur d'Apple ne marche qu'une fois l'accès accordé. C'est un écart avec le brief.
