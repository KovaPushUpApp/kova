# Ce que seul Julien peut faire

Liste des actions humaines (brief, section 12), dans l'ordre où elles bloquent.
Rien de ce qui suit n'est simulé dans le code.

## Compilation

**Fait le 17 novembre 2026** : l'app et ses 3 extensions compilent sur Codemagic, 46 tests verts
(workflow « Kova - Compilation et tests »). Rien n'a encore tourné sur un vrai iPhone.

## Pour installer l'app sur un iPhone

| # | Action | Où | Bloque |
|---|---|---|---|
| 1 | Compte Apple Developer actif | developer.apple.com | Tout build signé |
| 2 | **Fait le 16 novembre 2026** : les 4 identifiants et l'App Group (liste en tête de `project.yml`) | Certificates, Identifiers & Profiles | Tout build signé |
| 3 | Family Controls (Distribution) : **demande envoyée le 16 novembre 2026**, numéro B853HD2J39. Attendre la réponse d'Apple par e-mail | developer.apple.com, onglet « Capability Requests » de l'identifiant | Le verrouillage réel sur TestFlight, et la mise en ligne. Délai : de 1 jour à plus d'un mois |
| 4 | **Fait le 16 novembre 2026** : fiche « Kova: Bloqueur d'applications » | App Store Connect | TestFlight |
| 5 | Créer la clé API App Store Connect et la donner à Codemagic sous le nom `codemagic` | App Store Connect, puis Codemagic | TestFlight |
| 6 | Créer la variable `CERTIFICATE_PRIVATE_KEY` (groupe `signature`, cochée « Secure ») avec le contenu du fichier `livraison/kova_cert_key` (créé le 17 novembre 2026, hors dépôt, à garder en lieu sûr) | Codemagic | TestFlight |
| 7 | Avoir un iPhone sous iOS 16 ou plus | | Tous les tests réels |

## Pour les abonnements

| # | Action | Détail |
|---|---|---|
| 8 | Créer le groupe « Kova Premium » et 2 produits dans App Store Connect | `com.getkova.app.annuel` : 29,99 € avec essai gratuit de 3 jours. `com.getkova.app.hebdo` : 4,99 €. L'offre de rattrapage est coupée pour la version 1.0 (risque de refus, règle 5.6) : pas de troisième produit pour l'instant |
| 9 | Créer le projet RevenueCat | Un droit nommé `premium`. Offre courante : forfaits annuel et hebdomadaire |
| 10 | Copier la clé publique iOS de RevenueCat dans `KOVA_REVENUECAT_CLE` (`project.base.yml`) | Sans elle, les achats sont indisponibles dans l'app |
| 11 | Trancher la précommande "fondateur à vie" (brief, point ouvert 1) | Un code saisi dans l'app est interdit par Apple. Piste : un achat "à vie" dans l'app, offert aux fondateurs par codes d'offre Apple. À décider avant de vendre |

## Pour la mise en ligne

| # | Action | Détail |
|---|---|---|
| 12 | Faire valider `confidentialite.md` et `conditions.md` par un juriste | **Publiés le 17 novembre 2026** sur kova-site-theta.vercel.app, adresses dans `project.base.yml`. Restent la relecture, le téléphone et le médiateur |
| 13 | Si voulu, renseigner `KOVA_TELEMETRYDECK_ID` | `project.base.yml`. Le contact est renseigné |
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
