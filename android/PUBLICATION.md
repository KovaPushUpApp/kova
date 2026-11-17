# Publier Kova sur Google Play

## Construire

```
cd android
./gradlew bundleRelease
```

Le fichier à envoyer à Google Play est `app/build/outputs/bundle/release/app-release.aab`.
Avant chaque nouvel envoi, augmenter `versionCode` dans `app/build.gradle.kts`.

## La clé de signature

- `android/kova-publication.jks` : la clé.
- `android/keystore.properties` : son mot de passe.

Ces deux fichiers ne sont pas dans Git. **Copie-les dans un endroit sûr** (gestionnaire de
mots de passe, disque externe). Sans eux, tu ne peux plus mettre l'app à jour.
À la création de l'app, accepte "Signature d'application Play" : Google garde alors la clé
finale, et celle-ci ne sert plus qu'à envoyer les mises à jour. Elle peut être remplacée en cas de perte.

## Ce que toi seul peux faire

| # | Action | Détail |
|---|---|---|
| 1 | Créer le compte Google Play Console | 25 $, une fois. Vérification d'identité demandée |
| 2 | Test fermé | Un compte personnel récent doit faire tester l'app par 12 personnes pendant 14 jours avant la mise en ligne. À vérifier dans la console, la règle évolue |
| 3 | Politique de confidentialité et conditions | Fait : en ligne sur kova-site-theta.vercel.app, adresses dans `Liens` (`ecrans/Outils.kt`). À changer si le site prend un nom de domaine |
| 4 | Déclarer le service d'accessibilité | Texte et scénario de la vidéo plus bas |
| 5 | Remplir "Sécurité des données" | Réponses plus bas |
| 6 | Fiche, captures, classification du contenu | Textes de la fiche dans `playstore/` (titre, description courte, description longue, en français et en anglais) |
| 7 | Créer les abonnements | Annuel 29,99 € avec 3 jours d'essai, hebdomadaire 4,99 €, offre de rattrapage à 19,99 € la première année. Me le dire ensuite : les achats ne sont pas encore branchés dans l'app |

## Ce qui reste à coder

- **Les achats.** Dans la version publiée, le bouton d'abonnement affiche "achats indisponibles".
  Il faudra redonner l'accès Internet à l'app (retiré dans `AndroidManifest.xml`) et afficher
  les prix venus de Google Play au lieu des prix écrits dans les textes.
- **Le comptage** rate encore des pompes sur un vrai téléphone. Seuils à régler avec des mesures réelles.

## Statistiques d'usage (PostHog, région Union européenne)

L'app note ce qui s'y passe (écrans vus, étapes de l'onboarding, séances, paywall) dans
`Mesures.kt`. Tout est prêt pour l'envoi à PostHog, mais **rien ne part aujourd'hui** :
il manque la clé du projet et l'accès Internet.

Pour activer l'envoi :

1. Créer un compte sur PostHog en choisissant la région **EU**, puis un projet "Kova".
2. Mettre la clé du projet dans `android/mesures.properties` : `cle=phc_...`
3. Dans `AndroidManifest.xml`, remplacer la ligne qui retire `INTERNET` par la permission elle-même.
4. Dans PostHog, vérifier que la capture de l'adresse IP est désactivée (réglage du projet).

Ce que fait l'app une fois la clé en place :

- Elle pose la question juste après l'écran d'accueil, avec deux boutons identiques. Sans "oui", rien n'est envoyé.
- Un refus efface l'identifiant et ce qui attendait d'être envoyé. Le choix se change dans les réglages.
- Jamais le nom des apps verrouillées, jamais rien de la caméra. Identifiant tiré au hasard, sans fiche de personne ni localisation.

À faire en même temps, avant de publier avec Internet :

- **La bibliothèque de détection de posture (ML Kit) envoie ses propres statistiques à Google dès que l'app a Internet.**
  C'est pour ça que la permission était retirée. Lire la page de Google sur les données collectées par ML Kit et la déclarer.
- Mettre à jour la politique de confidentialité (`appstore/confidentialite.md`) : PostHog, les événements, le consentement.
- Mettre à jour "Sécurité des données" plus bas : l'app collectera des données d'usage, facultatives.
- Dans la déclaration du service d'accessibilité, retirer la phrase "the app has no Internet permission".

## Déclaration du service d'accessibilité

Google demande, pour toute app qui utilise l'API d'accessibilité sans être un outil
d'accessibilité : une déclaration dans la console, une vidéo, et un écran d'information
dans l'app avant l'activation. Cet écran existe déjà : "Autorise le verrouillage".
Le service est déclaré avec `isAccessibilityTool="false"`.

### Texte à coller dans la console (anglais)

> Kova is a self-control app. The user picks the apps they want to lock, and unlocks them by
> doing push-ups counted by the camera. Kova uses the AccessibilityService API for one purpose:
> to detect which app comes to the foreground, so it can show its lock screen over the apps the
> user chose.
>
> The service only listens to window state change events and reads the package name of the
> foreground app. It cannot read screen content (canRetrieveWindowContent is false), does not
> read text typed by the user, and performs no action on the user's behalf. It also shows a
> small countdown overlay during the last minute of unlocked time.
>
> Nothing is collected or shared: the app has no Internet permission. The list of locked apps
> stays on the device. Before enabling the service, the app shows a screen that explains this
> use, and the user enables the service themselves in Android settings. They can turn it off
> at any time.

### Scénario de la vidéo (environ 1 minute)

1. Ouvrir Kova, montrer l'écran "Autorise le verrouillage" et ses trois arguments.
2. Toucher "Ouvrir les réglages", activer le service Kova dans les réglages d'accessibilité.
3. Revenir dans Kova, choisir une app à verrouiller.
4. Ouvrir cette app : l'écran de blocage de Kova apparaît.
5. Faire une série, revenir : l'app s'ouvre, la notification affiche le temps restant.
6. Montrer le minuteur de la dernière minute, puis le reverrouillage.

## Sécurité des données

- Données collectées : **aucune**.
- Données partagées : **aucune**.
- L'app n'a pas accès à Internet. L'image de la caméra est analysée sur le téléphone, jamais
  enregistrée ni envoyée.
- Liste des apps installées : lue sur le téléphone pour proposer les apps à verrouiller, jamais envoyée.

Ces réponses devront changer le jour où les achats ou des statistiques d'usage seront ajoutés.
