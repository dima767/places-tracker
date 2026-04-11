#!/bin/bash
set -euo pipefail

# Stop only the MongoDB container on exe.dev VM
# Usage: ./stop-db-exe.sh        (stop, keep volume)
#        ./stop-db-exe.sh -v     (stop and remove volume - destroys DB!)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
    exit 1
fi
source "$CONFIG_FILE"

SSH_TARGET="${VM_USER}@${VM_HOST}"

if [ "${1:-}" = "-v" ]; then
    echo "WARNING: This will destroy the database and all its data!"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted"
        exit 0
    fi
    echo "Stopping MongoDB and removing volume on ${VM_HOST}..."
    ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml rm -sfv mongodb"
    ssh "$SSH_TARGET" "docker volume rm placestracker-prod_placestracker_mongodata 2>/dev/null || true"
    ssh "$SSH_TARGET" "docker volume rm placestracker-prod_placestracker_mongoconfig 2>/dev/null || true"
    echo "MongoDB stopped and volume removed"
else
    echo "Stopping MongoDB on ${VM_HOST}..."
    ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml stop mongodb"
    echo "MongoDB stopped (volume preserved)"
fi
