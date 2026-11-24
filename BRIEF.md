# Kova : brief complet de l'app

Document de référence pour construire l'app. Tout ce qui est décidé est ici. Ce qui ne l'est pas est listé en section 13.

## 1. Le concept en une phrase

Kova verrouille les apps qui te distraient (Instagram, TikTok, YouTube...) et ne les rouvre que si tu fais tes pompes, comptées en direct par la caméra.

- Je donne : des pompes devant mon téléphone.
- J'obtiens : des minutes d'accès à mes apps verrouillées.

## 2. Cible et positionnement

- Cible : 16 à 30 ans, qui scrollent trop et veulent se remettre au sport. France d'abord, anglais ensuite.
- Promesse : "Gagne ton temps d'écran."
- Ton : direct, tutoiement, phrases courtes, un peu de défi. Jamais moralisateur.
- Références de niche (à étudier, pas à copier) : PushUp Time, Pushscroll, Unrot.
- Angles de différenciation issus des avis concurrents :
  1. Un paywall honnête, avec un bouton fermer toujours visible.
  2. Un comptage plus strict, difficile à tromper avec de faux mouvements.
  3. Une app pensée et écrite en français.

## 3. Nom et référencement

- Nom de base : Kova
- Titre App Store (30 max) : `Kova: Bloqueur d'applications`
- Sous-titre (30 max) : `Pompes contre temps d'écran`
- Mots-clés (100 max) : `blocage,limiter,concentration,focus,scroll,addiction,téléphone,sport,squats,discipline,détox,digital`
- Catégorie principale : Productivité. Secondaire : Forme et santé.
- Ne jamais mettre de nom de marque tierce (TikTok, Instagram) dans les mots-clés.

## 4. Prix et modèle

Abonnement, géré avec RevenueCat.

| Formule | Prix | Détail |
|---|---|---|
| Annuel | 29,99 € | 3 jours d'essai gratuit. Présélectionné sur le paywall. Affiché "soit 0,58 € par semaine". |
| Hebdomadaire | 4,99 € | Sans essai. Sert d'ancre de prix. |
| Annuel, offre de rattrapage | 19,99 € la première année, puis 29,99 € | Affichée une seule fois, quand l'utilisateur ferme le paywall sans payer. |

Règles du paywall :
- Il arrive après l'essai des 3 pompes (l'utilisateur a vu que le comptage marche avant de payer).
- Bouton fermer visible dès l'affichage, sans délai.
- Bouton "Restaurer" visible.
- Mentions de prix et de renouvellement lisibles sous le bouton.
- Sans abonnement actif : l'utilisateur peut verrouiller 1 seule app et utiliser uniquement les pompes. Tout le reste (apps illimitées, 5 exercices, planning, stats) demande l'abonnement.

Les prix sont un point de départ à tester (A/B via RevenueCat), pas une vérité.

## 5. Périmètre de la V1

Dans la V1 :
- Onboarding complet (11 écrans, paywall compris).
- Sélection des apps à verrouiller.
- Verrouillage réel des apps via l'API Temps d'écran d'Apple.
- Écran de verrou personnalisé sur les apps bloquées.
- Séance d'exercice avec comptage par caméra, sur l'appareil.
- Exercices : pompes et squats d'abord. Abdos, fentes et jumping jacks ensuite, dans cet ordre, seulement si le comptage est fiable.
- Solde de minutes, fenêtre de déblocage, reverrouillage automatique.
- Déblocage d'urgence.
- Planning de plages strictes.
- Stats de la semaine et série de jours.
- Réglages, abonnement, restauration des achats.
- Français et anglais.

Hors V1 :
- Android.
- Compte utilisateur, connexion, synchronisation cloud (tout reste sur le téléphone).
- Classements, amis, partage social.
- Apple Watch, widgets.
- Temps d'usage par app affiché sur l'accueil (voir section 9, contrainte 3).

## 6. Les règles du jeu

1. **La règle** : X répétitions donnent Y minutes. Par défaut 10 pompes = 15 minutes. Réglable par pas de 5 (5 à 100 répétitions, 5 à 120 minutes).
2. **Taux par exercice pour 15 minutes** : 10 pompes, 15 squats, 15 abdos, 12 fentes, 25 jumping jacks. Si l'utilisateur change sa règle, les autres exercices suivent le même rapport.
3. **Le solde** : les minutes gagnées vont dans un solde. Après une séance, l'utilisateur choisit "Ouvrir maintenant" (une fenêtre de déblocage démarre) ou "Garder mon temps pour plus tard" (les minutes restent au solde).
4. **La fenêtre de déblocage** : pendant la fenêtre, toutes les apps verrouillées sont accessibles. Le temps s'écoule en temps réel, que l'app soit ouverte ou non. À la fin, les apps se reverrouillent toutes seules, même si Kova est fermée.
5. **Planning** : pendant une plage stricte active, les apps restent verrouillées même avec du solde. Seul le déblocage d'urgence passe.
6. **Déblocage d'urgence** : 5 minutes sans exercice, 2 fois par semaine maximum (remise à zéro le lundi). Désactivable dans les réglages.
7. **Anti-craquage** : retirer une app de la liste ou désactiver une plage prend effet 24 h plus tard. Durcir prend effet tout de suite. La règle (répétitions et minutes) change tout de suite, dans les deux sens : décision du 24 novembre 2026, après la sortie de la version 1.0.0.
8. **Série** : un jour compte dans la série si au moins une séance a été validée.
9. **Comptage** : une répétition ne compte que si le mouvement est complet (descente puis remontée). Une séance abandonnée ne donne rien.
10. **Confidentialité** : l'image de la caméra est analysée sur le téléphone, jamais enregistrée ni envoyée. C'est une promesse affichée à l'utilisateur, elle doit être vraie dans le code.

## 7. Les écrans

Les maquettes sont dans `maquettes/`. Chaque fichier `.dc.html` est un écran de 390 × 844 px. Les styles en ligne donnent les valeurs exactes (tailles, couleurs, espacements) et le bloc `<script>` en bas décrit la logique d'état. Les `{{accent}}` valent `#C8F53C`.

### Onboarding et paywall

| # | Fichier | Rôle | Contenu et logique | Suite |
|---|---|---|---|---|
| 1 | Bienvenue | Accroche | Logo, "Gagne ton temps d'écran." | Temps |
| 2 | Temps | Question | "Combien de temps par jour sur ton téléphone ?" 4 choix, un seul sélectionnable. Enregistrer la réponse. | Choc |
| 3 | Choc | Prise de conscience | Compteur qui monte jusqu'au nombre de jours par an, calculé : heures par jour × 365 / 24. Exemple : 5 h donnent 76 jours. Barre "21 % de ton année". | Objectif |
| 4 | Objectif | Question | "Tu veux quoi, au fond ?" Choix multiples. Enregistrer. | Apps |
| 5 | Apps | Sélection | Choix des apps à verrouiller. Voir contrainte 2, section 9 : utiliser le sélecteur système. | Regle |
| 6 | Regle | Réglage | Deux compteurs + et - : répétitions et minutes. Ligne calculée "Soit N pompes pour 1 h de scroll". | AutoEcran |
| 7 | AutoEcran | Autorisation | Explication puis demande d'accès Temps d'écran. Si refus : expliquer et proposer de réessayer, on ne peut pas continuer sans. | AutoCamera |
| 8 | AutoCamera | Autorisation | Explication puis demande d'accès caméra. | Essai |
| 9 | Essai | Moment de preuve | L'utilisateur fait 3 vraies pompes, comptées en direct. Le bouton "Continuer" n'apparaît qu'après la 3e. | Paywall |
| 10 | Paywall | Vente | Voir section 4. | Accueil |
| 11 | Offre | Rattrapage | Plein écran couleur, "-33 %", 19,99 €. Une seule fois. | Accueil |

### Cœur de l'app

| # | Fichier | Rôle | Contenu et logique |
|---|---|---|---|
| 12 | Main | Accueil | Solde de minutes en très gros, série, bouton "Gagner du temps", liste des apps verrouillées, barre d'onglets. |
| 13 | Verrou | App verrouillée | Écran affiché par dessus une app bloquée. "10 pompes, 15 min". Actions : commencer, fermer l'app, déblocage d'urgence. Voir contrainte 4. |
| 14 | Exercice | Choix | Liste des exercices avec le nombre de répétitions demandé. |
| 15 | Placement | Mise en place | "Pose ton téléphone au sol", à 1 m. Passe à la suite quand le corps entier est détecté. |
| 16 | Compteur | Séance | Vue caméra (38 % de la hauteur), compteur géant, barre de progression à segments, message d'encouragement. Bouton de validation seulement à la fin. |
| 17 | Gagne | Récompense | Plein écran couleur, "+15 min" qui défile, deux stats. Choix : ouvrir maintenant ou garder. |
| 18 | Urgence | Déblocage d'urgence | Explication, "1 sur 2 restant cette semaine", bouton "Débloquer 5 minutes". |

### Onglets et réglages

| # | Fichier | Rôle | Contenu et logique |
|---|---|---|---|
| 19 | Stats | Semaine | Total de répétitions, barres par jour (animation de croissance), 4 cartes de stats. |
| 20 | Planning | Plages strictes | Liste de plages avec interrupteur, bouton "Ajouter une plage". |
| 21 | Reglages | Réglages | Trois groupes : règles, options, compte. |
| 22 | AppsReglage | Modifier les apps | Même sélection que l'onboarding, avec le délai de 24 h pour les retraits. |
| 23 | Abonnement | Formule | Formule actuelle, renouvellement, lien vers la gestion App Store, restauration. |

`Icone.dc.html` : l'icône de l'app. Logo retenu : un cadenas ouvert avec un chevron vers le haut à l'intérieur, noir `#0E0E10` sur fond vert citron `#C8F53C`. Le tracé SVG exact est dans ce fichier.

## 8. Design

Toutes les valeurs sont dans `design-tokens.json`. Le système repose sur le nombre d'or (1,618) et la suite de Fibonacci.

- **Couleurs** : fond `#0E0E10`, cartes `#1A1A1E`, lignes `#2C2C33`, texte `#F4F4F1`, texte secondaire `#A3A3AB`, accent `#C8F53C`. Texte sur accent : `#0E0E10`.
- **Polices** : Space Grotesk 700 pour les titres et les chiffres, Inter 400/500/600 pour le reste.
- **Tailles de texte** : 13, 16, 20, 26, 33, 42, 68, 110, 178.
- **Espacements** : 3, 5, 8, 13, 21, 34, 55, 89, 144. Marges d'écran 21, haut 55, bas 34.
- **Arrondis** : 8, 13, 21, 34.
- **Hauteurs** : boutons principaux, options et onglets à 55. Petits boutons icône à 44 (minimum tactile).
- **Interligne** des paragraphes : 1,618.
- **Mode sombre uniquement** en V1.
- Deux écrans passent en plein fond accent : Gagne et Offre. C'est voulu, ce sont les moments forts.

Animations (courtes, 0,5 à 0,6 s, courbe ease) :
- Entrée des blocs : fondu + montée de 18 px, en cascade (décalage de 0,07 à 0,15 s).
- Logo, cadenas, gros chiffres : apparition "pop" (échelle 0,6 vers 1,06 puis 1).
- Bouton principal de l'accueil et du paywall : pulsation lente.
- Compteurs : défilement des chiffres (Choc, Gagne).
- Barres de stats : croissance depuis le bas.
- Écrans d'autorisation et verrou : anneau qui s'élargit autour de l'icône.
- Vue caméra : ligne de scan verticale.
- Respecter le réglage système "Réduire les animations".

Retours haptiques : léger à chaque répétition comptée, fort à la validation de la série.

Accessibilité : contrastes AA, zones tactiles de 44 minimum, libellés VoiceOver sur les boutons icône, textes qui supportent la taille dynamique.

## 9. Technique

### Stack recommandée

- **iOS natif, Swift et SwiftUI**, iOS 16 minimum. L'API Temps d'écran est native et demande des extensions : un framework multiplateforme compliquerait tout pour rien.
- **Projet généré avec XcodeGen** (`project.yml`), pour pouvoir tout écrire depuis Windows et générer le projet Xcode dans la CI.
- **Build et signature dans le cloud** (Codemagic ou équivalent), distribution par TestFlight.
- **RevenueCat** pour les abonnements, l'essai, l'offre de rattrapage et les tests de prix.
- **Stockage local** uniquement (UserDefaults partagé via App Group, ou SwiftData). Pas de serveur.
- **Analytics** : un outil léger et respectueux (TelemetryDeck ou PostHog), sans identifiant publicitaire.

### Cibles du projet

1. L'app principale.
2. Extension DeviceActivityMonitor : reverrouille à la fin d'une fenêtre, applique les plages du planning.
3. Extension ShieldConfiguration : dessine l'écran de verrou aux couleurs de Kova.
4. Extension ShieldAction : gère les boutons de l'écran de verrou.
5. Un App Group commun, pour partager le solde, la règle et l'état entre l'app et les extensions.

Frameworks Apple : FamilyControls, ManagedSettings, DeviceActivity, Vision, AVFoundation.

### Contraintes Apple à respecter (à vérifier dans la documentation à jour)

1. **Autorisation Family Controls** : la capacité doit être demandée à Apple pour la distribution, pour l'app et pour chaque extension. Sans elle, pas de mise en ligne. Démarche humaine, à lancer en premier.
2. **Les apps choisies sont opaques** : le sélecteur système (FamilyActivityPicker) renvoie des jetons, pas des noms. On ne peut pas afficher une liste maison avec des interrupteurs comme sur la maquette `Apps`. Adapter : garder le titre et le bouton de la maquette, ouvrir le sélecteur système au milieu, et afficher les apps choisies avec le composant Label fourni par Apple.
3. **Le temps d'usage par app** ("1 h 12 aujourd'hui") n'est accessible que dans une extension de rapport dédiée. En V1, retirer cette ligne des cartes d'apps.
4. **L'écran de verrou est limité** : titre, sous-titre, icône, couleurs et deux boutons au maximum. Il ne peut pas reproduire la maquette `Verrou` au pixel près, ni ouvrir Kova directement. Adapter : bouton principal "Faire mes pompes" qui envoie une notification à toucher pour ouvrir Kova sur la séance, bouton secondaire "Fermer". Le déblocage d'urgence se fait dans Kova.
5. **Durée minimale des fenêtres** : les intervalles DeviceActivity ont une durée minimale (15 minutes à ma connaissance). Vérifier, et caler le pas minimal de la règle dessus si besoin.
6. **Test sur vrai iPhone obligatoire** : le verrouillage ne se teste pas correctement dans le simulateur.
7. **Caméra** : texte d'autorisation clair dans Info.plist, et aucune image ne sort du téléphone.
8. **Abonnements** : respecter les règles d'affichage du prix, de la durée et du renouvellement, plus des liens vers les conditions et la politique de confidentialité.

### Comptage des répétitions

- Vision, détection de pose du corps, sur l'appareil, caméra avant, téléphone posé au sol.
- Pompe : suivre l'angle du coude. Bas quand l'angle passe sous 90°, haut quand il repasse au-dessus de 160°. Une répétition = un passage bas puis haut.
- Vérifier l'alignement épaules, hanches, chevilles pour refuser les faux mouvements (juste bouger la tête ou les bras).
- Squat : même principe avec l'angle du genou et la hauteur des hanches.
- Lisser les angles sur quelques images pour éviter les doubles comptages.
- Si le corps sort du cadre : mettre en pause et afficher "Reviens dans le cadre".
- Garder l'écran allumé pendant la séance.

## 10. Données à suivre

Événements : onboarding_etape_vue (avec le numéro), autorisation_accordee ou refusee (écran, caméra), essai_reussi, paywall_vu, paywall_ferme, offre_vue, achat (formule), seance_commencee, seance_validee (exercice, répétitions), seance_abandonnee, fenetre_ouverte, urgence_utilisee, regle_modifiee.

Les trois chiffres qui comptent : taux de passage onboarding vers paywall, taux de conversion paywall, rétention à 7 jours.

## 11. Critères d'acceptation

L'app est prête quand tout ceci est vrai sur un vrai iPhone :

1. Un nouvel utilisateur va de l'ouverture au paywall sans blocage, en moins de 3 minutes.
2. Une app choisie est réellement bloquée, même après redémarrage du téléphone.
3. 10 pompes correctes sont comptées 10, à plus ou moins 1. Dix faux mouvements (tête seule) sont comptés 0.
4. Après une séance, les apps s'ouvrent, puis se reverrouillent seules à la fin de la fenêtre, Kova fermée.
5. Le planning bloque bien pendant ses plages.
6. L'urgence marche 2 fois par semaine, pas 3.
7. Retirer une app ne prend effet que 24 h plus tard.
8. Achat, essai, restauration et offre de rattrapage fonctionnent en bac à sable.
9. Tous les textes existent en français et en anglais.
10. Chaque écran correspond à sa maquette (hors adaptations de la section 9).
11. Aucune image de la caméra n'est écrite sur disque ni envoyée sur le réseau.

## 12. Ce que seul Julien peut faire

1. Compte Apple Developer actif.
2. Créer la fiche de l'app dans App Store Connect (cela réserve le nom Kova).
3. Demander l'autorisation Family Controls à Apple, pour l'app et ses extensions.
4. Créer le projet RevenueCat et les produits d'abonnement dans App Store Connect.
5. Relier le dépôt à la CI et fournir les certificats de signature.
6. Tester chaque build sur son iPhone via TestFlight.
7. Rédiger ou faire valider les conditions d'utilisation et la politique de confidentialité.
8. Vérifier la marque Kova à l'INPI.

## 13. Points ouverts

1. **Précommande "fondateur à vie" à 24,99 €** vendue sur la landing : débloquer dans l'app un accès acheté hors App Store peut enfreindre les règles d'Apple. Piste à valider : livrer la précommande sous forme de code d'offre Apple ou d'accès promotionnel RevenueCat. À trancher avant de vendre.
2. **Sans abonnement** : la version limitée (1 app, pompes seules) est une proposition. Le concurrent, lui, impose l'abonnement. À tester.
3. **Nombre d'exercices au lancement** : 2 fiables valent mieux que 5 approximatifs.
4. **Nom "Kova"** : d'autres apps portent ce nom dans d'autres catégories. Marque à vérifier.
