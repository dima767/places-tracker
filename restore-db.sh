#!/bin/bash
set -euo pipefail

# Restore a mongodump archive (--archive --gzip) into a target Mongo instance.
# Three targets supported:
#
#   ./restore-db.sh <file>              # restore into LOCAL container (placestracker-mongodb)
#   ./restore-db.sh <file> --vm         # restore into VM container (via SSH)
#   ./restore-db.sh <file> --uri <URI>  # restore into an arbitrary Mongo URI (needs mongorestore installed)
#
# Always passes --drop so existing data is replaced.
#
# Expected input format: mongodump --archive --gzip (what backup-db.sh produces)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ $# -lt 1 ]; then
    echo "Usage:"
    echo "  $0 <file>                      # restore into local container"
    echo "  $0 <file> --vm                 # restore into VM container"
    echo "  $0 <file> --uri <mongo-uri>    # restore into specific Mongo URI"
    exit 1
fi

FILE="$1"
shift

if [ ! -f "$FILE" ]; then
    echo "ERROR: file not found: $FILE"
    exit 1
fi

MODE="local"
URI=""
case "${1:-}" in
    --vm) MODE="vm" ;;
    --uri)
        MODE="uri"
        URI="${2:?--uri requires a Mongo URI argument}"
        ;;
    "") ;;
    *) echo "ERROR: unknown option '$1'"; exit 1 ;;
esac

SIZE=$(du -h "$FILE" | cut -f1)
echo "=== Restoring ${FILE} (${SIZE}) ==="

case "$MODE" in
    local)
        echo "Target: local container placestracker-mongodb"
        if ! docker ps --format '{{.Names}}' | grep -q '^placestracker-mongodb$'; then
            echo "ERROR: local container 'placestracker-mongodb' is not running."
            echo "Start it first with: ./compose-up-standalone.sh  (or compose-up.sh for dev mode)"
            exit 1
        fi
        echo ""
        docker exec -i placestracker-mongodb mongorestore --gzip --archive --drop < "$FILE"
        ;;
    vm)
        CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"
        [ -f "$CONFIG_FILE" ] || { echo "ERROR: .deploy-config not found"; exit 1; }
        source "$CONFIG_FILE"
        SSH_TARGET="${VM_USER}@${VM_HOST}"
        echo "Target: VM container placestracker-mongodb via ${SSH_TARGET}"
        echo ""
        ssh "$SSH_TARGET" 'docker exec -i placestracker-mongodb mongorestore --gzip --archive --drop' < "$FILE"
        ;;
    uri)
        echo "Target: ${URI}"
        if ! command -v mongorestore >/dev/null 2>&1; then
            echo "ERROR: mongorestore is not installed."
            echo "Install: brew install mongodb-database-tools"
            exit 1
        fi
        echo ""
        mongorestore --gzip --archive="$FILE" --drop --uri="$URI"
        ;;
esac

echo ""
echo "=== Done ==="
