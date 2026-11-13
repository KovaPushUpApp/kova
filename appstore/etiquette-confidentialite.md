# Étiquette de confidentialité (App Store Connect)

Réponses à saisir dans "Confidentialité de l'app". Elles décrivent ce que fait le code,
SDK compris. À revoir si un SDK est ajouté ou retiré.

## Données collectées

| Donnée | Collectée par | Usage | Liée à l'utilisateur | Suivi publicitaire |
|---|---|---|---|---|
| Achats : historique des achats | RevenueCat | Fonctionnalité de l'app, Analyse | Non | Non |
| Identifiants : identifiant de l'appareil | TelemetryDeck (identifiant haché) | Analyse | Non | Non |
| Données d'utilisation : interactions avec le produit | TelemetryDeck | Analyse | Non | Non |

Si `KOVA_TELEMETRYDECK_ID` reste vide, les deux lignes TelemetryDeck ne s'appliquent pas.

## Données non collectées

- Image de la caméra : analysée sur le téléphone, jamais enregistrée ni envoyée.
- Apps choisies : Kova ne reçoit que des jetons opaques, qui restent sur le téléphone.
- Temps d'usage des apps : Kova n'y a pas accès.
- Nom, e-mail, contacts, position : Kova ne les demande pas. Il n'y a pas de compte.

## Suivi

L'app ne suit pas l'utilisateur. Pas d'identifiant publicitaire, pas de demande de suivi.
