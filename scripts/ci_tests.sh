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

# Le simulateur démarre avant les tests : sinon xcodebuild l'attend en silence.
xcrun simctl boot "$SIMULATEUR" || true
xcrun simctl bootstatus "$SIMULATEUR" -b

REGLAGES=(-project Kova.xcodeproj -scheme Kova -sdk iphonesimulator
  -destination "id=$SIMULATEUR" -configuration Debug
  CODE_SIGN_IDENTITY= CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO)

xcodebuild build-for-testing "${REGLAGES[@]}"

# Les tests tournent sous surveillance : un test bloqué est coupé au bout d'une
# minute, et si tout se fige on affiche où l'app est arrêtée avant d'abandonner.
xcodebuild test-without-building "${REGLAGES[@]}" \
  -parallel-testing-enabled NO \
  -test-timeouts-enabled YES \
  -default-test-execution-time-allowance 60 \
  -maximum-test-execution-time-allowance 60 &
TESTS=$!
ATTENTE=0
while kill -0 "$TESTS" 2>/dev/null; do
  sleep 5
  ATTENTE=$((ATTENTE + 5))
  if [ "$ATTENTE" -ge 600 ]; then
    echo "Tests figés depuis 10 minutes. État de l'app :"
    for PROCESSUS in $(pgrep -f "Kova.app/Kova" || true); do
      sample "$PROCESSUS" 3 2>/dev/null | head -150 || true
    done
    kill "$TESTS" 2>/dev/null || true
    exit 1
  fi
done
wait "$TESTS"
