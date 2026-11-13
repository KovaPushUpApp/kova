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

- [ ] Autorisation Family Controls accordée pour les 4 identifiants, profils régénérés
- [ ] Build fait avec `project.yml` (verrouillage réel), pas `project.base.yml`
- [ ] Écran "Contrôle du socle" retiré, et avec lui "Simuler l'abonnement"
- [ ] Clé RevenueCat, lien de confidentialité et contact renseignés
- [ ] Abonnements "Prêts à soumettre" et joints à la version
- [ ] Fiche remplie depuis `appstore/fr` et `appstore/en`
- [ ] Captures : au moins une en 1320 × 2868 et une en 1206 × 2622, sans transparence
- [ ] Catégories : Productivité, puis Forme et santé
- [ ] Étiquette de confidentialité remplie (`etiquette-confidentialite.md`)
- [ ] Notes pour la revue collées (`notes-pour-la-revue.md`)
- [ ] Aucun nom de marque tierce dans les mots-clés ni dans les captures
- [ ] Classification par âge remplie
