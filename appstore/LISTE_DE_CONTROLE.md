# Liste de contrôle avant soumission

## État au 13 novembre 2026

Tout le code des étapes 1 à 8 est écrit. **Rien n'a été compilé ni exécuté.**
Aucun critère ci-dessous n'est donc validé. La colonne "Preuve" dit ce qui
permettra de cocher chaque ligne.

## Critères d'acceptation (brief, section 11)

À vérifier sur un vrai iPhone. Cocher seulement après l'avoir vu.

| # | Critère | Ce que le code fait | Preuve attendue |
|---|---|---|---|
| 1 | De l'ouverture au paywall sans blocage, en moins de 3 minutes | 9 écrans avant le paywall. Si la caméra est refusée, l'essai peut être passé | Chronométrer un premier lancement |
| 2 | Une app choisie est bloquée, même après redémarrage | Boucliers posés par l'API Temps d'écran. Leur tenue au redémarrage n'est pas documentée par Apple | Redémarrer l'iPhone, ouvrir l'app bloquée |
| 3 | 10 pompes comptées 10 (±1), 10 faux mouvements comptés 0 | Logique testée sur des mouvements dessinés (`KovaTests`). Seuils non réglés sur une vraie caméra | 10 pompes, puis 10 hochements de tête, devant l'iPhone |
| 4 | Reverrouillage seul à la fin de la fenêtre, Kova fermée | Réveil de l'extension à l'heure de fin. Des développeurs signalent des réveils manqués | Fermer Kova, attendre la fin, ouvrir l'app |
| 5 | Le planning bloque pendant ses plages | Testé en logique (`MoteurJeuTests`) | Créer une plage de 15 minutes et observer |
| 6 | L'urgence marche 2 fois par semaine, pas 3 | Testé en logique | Trois essais dans la semaine |
| 7 | Retirer une app prend effet 24 h plus tard | L'app retirée reste sous bouclier 24 h | Retirer une app, vérifier le lendemain |
| 8 | Achat, essai, restauration, rattrapage en bac à sable | Code RevenueCat écrit. Produits non créés | Compte bac à sable sur l'iPhone |
| 9 | Tous les textes en français et en anglais | 270 clés, contrôle automatique à chaque build | Passer l'iPhone en anglais |
| 10 | Chaque écran correspond à sa maquette | Valeurs tirées des tokens. Jamais vu à l'écran | Comparer écran par écran |
| 11 | Aucune image de la caméra écrite ou envoyée | Aucune sortie photo ou vidéo, aucune écriture, aucun envoi dans `Kova/Comptage` | Relecture du code, et test en mode avion |

## Points techniques à surveiller au premier essai réel

- Fenêtres de moins de 15 minutes : le reverrouillage repose sur un réveil de l'extension.
- Bouton "Faire mes pompes" de l'écran de verrou : il envoie une notification à toucher.
  Elle peut arriver avec quelques secondes de retard.
- Cadrage : à 1 mètre, le corps entier ne tient peut-être pas dans l'image. Le comptage
  n'exige que l'épaule, le coude et le poignet d'un côté.
- Barre d'état sur les écrans verts (Gagné, Offre).

## Soumission

Liste revue le 18 novembre 2026 d'après les règles d'Apple en vigueur (dernière mise à
jour des consignes : 16 octobre 2026). Dans l'ordre où ça bloque.

### Comptes et accords (toi)

- [ ] Accord « Apps payantes » actif, banque et fiscalité remplies. Sans lui, les achats ne marchent pas pendant la revue
- [ ] Statut « trader » (règlement DSA) déclaré et vérifié : adresse, e-mail et téléphone, affichés publiquement dans l'UE
- [ ] Family Controls (Distribution) accordé pour les **4** identifiants : l'app et chacune des 3 extensions. Vérifier dans « Capability Requests » que chaque identifiant a sa demande, statut « Assigned »

### Abonnements (App Store Connect)

- [ ] Groupe « Kova Premium », avec son nom affiché en français et en anglais
- [ ] `com.getkova.app.annuel` : 1 an, 29,99 €, essai gratuit de 3 jours
- [ ] `com.getkova.app.hebdo` : 1 semaine, 4,99 €
- [ ] Pour chacun : nom affiché et description en français et en anglais, capture d'écran pour la revue, statut « Prêt à soumettre »
- [ ] Les deux abonnements joints à la version 1.0 (« Ajouter pour la revue »). Le premier abonnement d'une app part forcément avec une version
- [ ] Pas d'offre de rattrapage dans la version 1.0 : coupée dans le code (`Abonnements.offreDeRattrapageActive`). Une remise affichée à la fermeture du paywall a déjà valu des refus (règle 5.6)
- [x] RevenueCat : droit `premium`, offre courante avec les forfaits annuel et hebdomadaire, clé `appl_` dans `KOVA_REVENUECAT_CLE`
- [ ] Achat, essai et restauration testés en bac à sable sur un iPhone : les prix s'affichent, l'achat aboutit

### Build

- [ ] Build fait avec `project.yml` (verrouillage réel), pas `project.base.yml`. L'écran « Contrôle du socle » n'y est pas compilé
- [ ] Construit avec Xcode 26 ou plus (exigé depuis le 28 avril 2026). Codemagic : `xcode: latest`
- [ ] Dans TestFlight, « Build Metadata » montre l'autorisation Family Controls sur l'app et les 3 extensions
- [ ] Testé sur un vrai iPhone : onboarding, refus de la caméra, refus de Temps d'écran, refus des notifications, verrou, séance, reverrouillage
- [ ] Essayé sur un iPad si possible : une app iPhone peut être testée par Apple sur iPad en mode compatibilité

### Fiche

- [ ] Textes depuis `appstore/fr` et `appstore/en`. La description contient les liens vers les conditions et la confidentialité
- [ ] Adresse de la politique de confidentialité et adresse d'assistance renseignées, pages en ligne
- [ ] Captures : un jeu en 6,9 pouces (1320 × 2868 ou 1290 × 2796) et un en 6,3 pouces (1206 × 2622 ou 1179 × 2556), sans transparence. Plus de la moitié montrent l'app en usage. Aucune icône d'app tierce
- [ ] Aucune mention d'Android ou de Google Play, aucun prix, aucun « gratuit », aucun nom de concurrent dans le nom, le sous-titre, les mots-clés et les captures
- [ ] Catégorie principale : Productivité. Si « Forme et santé » est mise en seconde, déclarer « Non » à la question du dispositif médical réglementé
- [ ] Classification par âge : tout le questionnaire, y compris les nouvelles questions (contrôles dans l'app, réseaux sociaux, position partagée, rencontres). « Santé ou bien-être » : oui (exercices). « Contrôle parental » : non
- [ ] Étiquette de confidentialité remplie (`etiquette-confidentialite.md`) : historique des achats, non lié à l'identité, pas de suivi
- [ ] Chine continentale décochée pour la version 1.0 (formalités à part)
- [ ] Notes pour la revue collées (`notes-pour-la-revue.md`), modèle d'iPhone et version d'iOS complétés, vidéo de démonstration jointe
- [ ] Coordonnées du contact pour la revue remplies
