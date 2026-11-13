#!/usr/bin/env bash
# Sauvegarde de la base PostgreSQL de CIC (pg_dump au format custom, restaurable avec pg_restore).
#
# Usage :
#   scripts/sauvegarde-base.sh            # une sauvegarde, tout de suite
#   scripts/sauvegarde-base.sh --quotidien # une sauvegarde au lancement, puis chaque jour à SAUVEGARDE_HEURE
#
# Connexion : variables standard de libpq (PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD) ; aucun secret ici.
# Configuration :
#   SAUVEGARDE_REPERTOIRE      répertoire des fichiers (défaut : ./sauvegardes)
#   SAUVEGARDE_RETENTION_JOURS sauvegardes plus anciennes supprimées (défaut : 14)
#   SAUVEGARDE_HEURE           heure de la sauvegarde quotidienne, HH:MM, fuseau du conteneur (défaut : 02:00)
#
# Restauration (base vide, application arrêtée) :
#   pg_restore --clean --if-exists --no-owner -d "$PGDATABASE" cic-AAAAMMJJ-HHMMSS.dump
set -euo pipefail

REPERTOIRE="${SAUVEGARDE_REPERTOIRE:-./sauvegardes}"
RETENTION_JOURS="${SAUVEGARDE_RETENTION_JOURS:-14}"
HEURE="${SAUVEGARDE_HEURE:-02:00}"
BASE="${PGDATABASE:-cic}"

journal() { printf '%s sauvegarde %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

sauvegarder() {
    mkdir -p "$REPERTOIRE"
    local fichier="$REPERTOIRE/${BASE}-$(date -u +%Y%m%d-%H%M%S).dump"
    local partiel="$fichier.partiel"
    # Écriture dans un fichier temporaire : une sauvegarde interrompue n'est jamais prise pour une bonne.
    if ! pg_dump --format=custom --compress=6 --no-owner --dbname="$BASE" --file="$partiel"; then
        rm -f "$partiel"
        journal "ECHEC pg_dump"
        return 1
    fi
    # Contrôle : le fichier doit être lisible par pg_restore.
    if ! pg_restore --list "$partiel" > /dev/null; then
        rm -f "$partiel"
        journal "ECHEC fichier illisible"
        return 1
    fi
    mv "$partiel" "$fichier"
    journal "OK $(basename "$fichier") ($(du -h "$fichier" | cut -f1))"
    find "$REPERTOIRE" -maxdepth 1 -name "${BASE}-*.dump" -mtime "+$RETENTION_JOURS" -print -delete \
        | while read -r ancien; do journal "suppression $(basename "$ancien")"; done
}

secondes_jusqu_a_la_prochaine() {
    local maintenant prochaine
    maintenant=$(date +%s)
    prochaine=$(date -d "today $HEURE" +%s)
    if (( prochaine <= maintenant )); then
        prochaine=$(date -d "tomorrow $HEURE" +%s)
    fi
    echo $(( prochaine - maintenant ))
}

case "${1:-}" in
    --quotidien)
        journal "service démarré : chaque jour à $HEURE, rétention $RETENTION_JOURS jours, dans $REPERTOIRE"
        sauvegarder || true
        while true; do
            sleep "$(secondes_jusqu_a_la_prochaine)"
            # Un échec est journalisé ; on retente le lendemain sans arrêter le service.
            sauvegarder || true
        done
        ;;
    "")
        sauvegarder
        ;;
    *)
        echo "Usage : $0 [--quotidien]" >&2
        exit 2
        ;;
esac
