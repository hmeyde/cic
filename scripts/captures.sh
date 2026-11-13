#!/usr/bin/env bash
# Captures d'écran FR/AR des écrans clés, dans docs/captures/<langue>/ (depuis la racine du dépôt).
# Prérequis : l'application tourne en profil demo, par exemple
#   ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
# Puis : bash scripts/captures.sh   (URL : CIC_CAPTURES_URL, défaut http://localhost:8080)
# Au premier lancement, Playwright télécharge Chromium dans ~/.cache/ms-playwright.
set -euo pipefail
cd "$(dirname "$0")/.."
URL="${CIC_CAPTURES_URL:-http://localhost:8080}"
if ! curl -fsS -o /dev/null "$URL/"; then
  echo "Application injoignable sur $URL : la démarrer en profil demo." >&2
  exit 1
fi
./mvnw -q -f scripts/captures/pom.xml compile exec:java
