#!/bin/bash
set -euo pipefail

# Backup VM MongoDB to a single portable file (gzipped mongodump archive).
# Streams over SSH - nothing written on the VM side.
#
# Usage:
#   ./backup-db.sh                  # write to ./backups/placestracker-YYYYMMDD-HHMMSS.archive.gz
#   ./backup-db.sh /path/to/dir     # write into the given directory
#   ./backup-db.sh /path/to/file.archive.gz   # write to exact path
#
# Restore anywhere with:
#   mongorestore --gzip --archive=<file> [--drop]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
    exit 1
fi
source "$CONFIG_FILE"

SSH_TARGET="${VM_USER}@${VM_HOST}"
DB_NAME="${MONGO_INITDB_DATABASE:-placestracker}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
DEFAULT_NAME="placestracker-${TIMESTAMP}.archive.gz"

# --- Resolve output path ---
ARG="${1:-}"
if [ -z "$ARG" ]; then
    OUT_DIR="${SCRIPT_DIR}/backups"
    OUT_FILE="${OUT_DIR}/${DEFAULT_NAME}"
elif [ -d "$ARG" ]; then
    OUT_DIR="$ARG"
    OUT_FILE="${OUT_DIR}/${DEFAULT_NAME}"
else
    OUT_DIR="$(dirname "$ARG")"
    OUT_FILE="$ARG"
fi

mkdir -p "$OUT_DIR"

echo "=== Backing up ${DB_NAME} from ${VM_HOST} ==="
echo "Output: ${OUT_FILE}"
echo ""

# Stream: VM mongo container -> ssh -> local file
# 2>&1 redirect inside ssh captures mongodump's progress to stderr so it shows live.
ssh "$SSH_TARGET" "docker exec placestracker-mongodb mongodump --db=${DB_NAME} --archive --gzip" > "$OUT_FILE"

SIZE=$(du -h "$OUT_FILE" | cut -f1)
echo ""
echo "=== Done ==="
echo "File: ${OUT_FILE}"
echo "Size: ${SIZE}"
echo ""
echo "Restore on any Mongo instance with:"
echo "  mongorestore --gzip --archive='${OUT_FILE}' --drop"
echo ""
echo "Or into a specific container:"
echo "  docker exec -i <mongo-container> mongorestore --gzip --archive --drop < '${OUT_FILE}'"
