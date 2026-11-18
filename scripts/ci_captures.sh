#!/bin/bash
# Captures d'écran de la fiche App Store, prises dans le simulateur.
#
# Compile l'app du socle (sans verrouillage, sans signature), puis la lance sur
# chaque écran avec le mode capture (Kova/Socle/ModeCapture.swift), en français
# et en anglais, sur un iPhone 6,9 pouces et un iPhone 6,3 pouces.
# Résultat : captures/<taille>/<langue>/<rang>-<écran>.png
#
# L'écran de la séance n'y est pas : le simulateur n'a pas de caméra.
set -euo pipefail

IDENTIFIANT="com.getkova.app"
SORTIE="$PWD/captures"
PRODUITS="$PWD/build/captures"
ECRANS=(accueil verrou gagne stats planning paywall)
LANGUES=(fr en)

xcodegen generate --spec project.base.yml

xcodebuild build -project Kova.xcodeproj -scheme Kova -sdk iphonesimulator \
  -configuration Debug -destination "generic/platform=iOS Simulator" \
  -derivedDataPath "$PRODUITS" ARCHS=arm64 ONLY_ACTIVE_ARCH=NO \
  CODE_SIGN_IDENTITY= CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO

APP="$PRODUITS/Build/Products/Debug-iphonesimulator/Kova.app"
if [ ! -d "$APP" ]; then
  echo "App introuvable : $APP"
  exit 1
fi

LISTE=$(xcrun simctl list devices available)
echo "$LISTE"

# Identifiant du premier simulateur dont le nom est exactement celui demandé.
simulateur() {
  printf '%s\n' "$LISTE" | awk -v nom="$1" '
    index($0, "    " nom " (") == 1 && !vu {
      if (match($0, /[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}/)) { print substr($0, RSTART, RLENGTH); vu = 1 }
    }'
}

# Premier simulateur disponible parmi les noms donnés.
choisir() {
  for NOM in "$@"; do
    TROUVE=$(simulateur "$NOM")
    if [ -n "$TROUVE" ]; then
      echo "$TROUVE"
      return 0
    fi
  done
  return 1
}

capturer() {
  local TAILLE="$1"
  local APPAREIL="$2"
  echo "Captures $TAILLE pouces sur $APPAREIL"
  xcrun simctl boot "$APPAREIL" || true
  xcrun simctl bootstatus "$APPAREIL" -b
  # Un simulateur neuf affiche des notifications du système dans la minute qui
  # suit son démarrage. On les laisse passer avant la première capture.
  sleep 60
  # Barre d'état propre : 9:41, batterie pleine, réseau complet.
  xcrun simctl status_bar "$APPAREIL" override --time "9:41" \
    --batteryState charged --batteryLevel 100 --wifiBars 3 --cellularBars 4 || true
  xcrun simctl install "$APPAREIL" "$APP"
  for LANGUE in "${LANGUES[@]}"; do
    local REGION="fr_FR"
    if [ "$LANGUE" = "en" ]; then REGION="en_US"; fi
    mkdir -p "$SORTIE/$TAILLE/$LANGUE"
    local RANG=1
    for ECRAN in "${ECRANS[@]}"; do
      xcrun simctl terminate "$APPAREIL" "$IDENTIFIANT" 2>/dev/null || true
      xcrun simctl launch "$APPAREIL" "$IDENTIFIANT" \
        -AppleLanguages "($LANGUE)" -AppleLocale "$REGION" -KovaCapture "$ECRAN"
      # Le temps que les animations d'entrée se terminent.
      sleep 6
      xcrun simctl io "$APPAREIL" screenshot "$SORTIE/$TAILLE/$LANGUE/0$RANG-$ECRAN.png"
      RANG=$((RANG + 1))
    done
  done
  xcrun simctl terminate "$APPAREIL" "$IDENTIFIANT" 2>/dev/null || true
  xcrun simctl shutdown "$APPAREIL" || true
}

GRAND=$(choisir "iPhone 17 Pro Max" "iPhone 16 Pro Max") || { echo "Aucun iPhone 6,9 pouces disponible"; exit 1; }
MOYEN=$(choisir "iPhone 17 Pro" "iPhone 16 Pro") || { echo "Aucun iPhone 6,3 pouces disponible"; exit 1; }

capturer "6.9" "$GRAND"
capturer "6.3" "$MOYEN"

# Tailles réelles des images : Apple attend 1320 x 2868 (6,9) et 1206 x 2622 (6,3).
for IMAGE in "$SORTIE"/*/*/*.png; do
  echo "$IMAGE : $(sips -g pixelWidth -g pixelHeight "$IMAGE" | awk '/pixel/ { printf "%s ", $2 }')"
done
