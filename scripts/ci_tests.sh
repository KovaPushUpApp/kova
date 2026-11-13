#!/bin/bash
# Compile le projet complet (app + 3 extensions) et lance les tests dans le
# simulateur, sans signature. Ne demande ni compte Apple ni iPhone.
# Utilisé par Codemagic et par GitHub Actions.
set -euo pipefail

python3 scripts/generate_tokens.py --check
python3 scripts/check_localisation.py

xcodegen generate --spec project.yml

# Premier simulateur d'iPhone disponible sur la machine.
LISTE=$(xcrun simctl list devices available)
SIMULATEUR=$(printf '%s\n' "$LISTE" | awk '/iPhone/ && !vu { if (match($0, /[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}/)) { print substr($0, RSTART, RLENGTH); vu=1 } }')
if [ -z "$SIMULATEUR" ]; then
  echo "Aucun simulateur d'iPhone disponible"
  exit 1
fi
echo "Simulateur : $SIMULATEUR"

xcodebuild test \
  -project Kova.xcodeproj \
  -scheme Kova \
  -sdk iphonesimulator \
  -destination "id=$SIMULATEUR" \
  -configuration Debug \
  CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO
