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
| 3 | Publier la politique de confidentialité et les conditions | Brouillons dans `appstore/`. Mettre ensuite les adresses dans `Liens` (`ecrans/Outils.kt`), avec l'adresse de contact |
| 4 | Déclarer le service d'accessibilité | Texte et scénario de la vidéo plus bas |
| 5 | Remplir "Sécurité des données" | Réponses plus bas |
| 6 | Fiche, captures, classification du contenu | Textes de la fiche dans `appstore/` |
| 7 | Créer les abonnements | Annuel 29,99 € avec 3 jours d'essai, hebdomadaire 4,99 €, offre de rattrapage à 19,99 € la première année. Me le dire ensuite : les achats ne sont pas encore branchés dans l'app |

## Ce qui reste à coder

- **Les achats.** Dans la version publiée, le bouton d'abonnement affiche "achats indisponibles".
  Il faudra redonner l'accès Internet à l'app (retiré dans `AndroidManifest.xml`) et afficher
  les prix venus de Google Play au lieu des prix écrits dans les textes.
- **Le comptage** rate encore des pompes sur un vrai téléphone. Seuils à régler avec des mesures réelles.

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
