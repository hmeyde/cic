#!/usr/bin/env bash
# Enchaîne les prompts de PROMPTS.md avec Claude Code, sans demande de permission.
# À lancer à la racine du dépôt, DANS un conteneur (devcontainer), jamais en root.
# Reprise automatique : un prompt déjà terminé est sauté.
# LAST_PROMPT=N : s'arrête après le prompt N.
set -euo pipefail

PROMPTS_FILE="PROMPTS.md"
WORK_DIR=".prompts"
mkdir -p "$WORK_DIR"

# 1. Découpe PROMPTS.md : chaque bloc ``` devient un fichier 01.txt, 02.txt, ...
awk -v dir="$WORK_DIR" '
  /^```$/ {
    if (inblock) { close(f); inblock = 0 }
    else { n++; f = sprintf("%s/%02d.txt", dir, n); inblock = 1 }
    next
  }
  inblock { print > f }
' "$PROMPTS_FILE"

# 2. Exécute chaque prompt dans une session neuve
for f in "$WORK_DIR"/*.txt; do
  n=$((10#$(basename "$f" .txt)))
  if [ -n "${LAST_PROMPT:-}" ] && [ "$n" -gt "$LAST_PROMPT" ]; then
    echo "=== Arrêt après le prompt $LAST_PROMPT (LAST_PROMPT) ==="
    break
  fi
  if [ -f "$f.done" ]; then
    echo "=== $(basename "$f") déjà terminé, je passe ==="
    continue
  fi

  avant=$(git rev-parse HEAD)
  echo "=== $(basename "$f") : démarrage $(date -u +%H:%M) ==="
  claude -p "$(cat "$f")

Consignes : respecte CLAUDE.md. Commence par lire docs/CARTE.md pour te repérer au lieu d'explorer tout le code. Ne me pose aucune question : fais le choix le plus raisonnable et note-le dans le message de commit. Pendant le travail, lance seulement les tests concernés (./mvnw -q test -Dtest=...) ; la suite complète une seule fois, juste avant le commit. Quand le critère « Terminé quand » est atteint et que ./mvnw test passe, fais un commit git avec un message clair écrit pour CE prompt (git commit -m, ou un fichier de message neuf : jamais un ancien fichier de /tmp)." \
    --dangerously-skip-permissions

  # 3. Garde-fou : un commit doit exister, et les tests doivent passer si le code a changé
  if [ "$(git rev-parse HEAD)" = "$avant" ]; then
    echo "!!! Aucun commit après $(basename "$f"). Arrêt : vérifie puis relance le script."
    exit 1
  fi
  if git diff --quiet "$avant" HEAD -- src pom.xml; then
    echo "--- $(basename "$f") : ni src/ ni pom.xml modifiés, suite de tests sautée"
  elif [ -x ./mvnw ] && ! ./mvnw -q test; then
    echo "!!! Les tests échouent après $(basename "$f"). Arrêt : corrige puis relance le script."
    exit 1
  fi

  touch "$f.done"
  echo "=== $(basename "$f") : terminé $(date -u +%H:%M) ==="
done

echo "Tous les prompts sont terminés."
