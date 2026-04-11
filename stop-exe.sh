#!/bin/bash
set -euo pipefail

# Places Tracker stop on exe.dev VM
# Stops containers. Use -v flag to also remove volumes (database data!)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
    echo "Run: cp .deploy-config.example .deploy-config and fill in your values"
    exit 1
fi
source "$CONFIG_FILE"

MISSING=()
[ -z "${VM_HOST:-}" ] && MISSING+=("VM_HOST")
[ -z "${VM_USER:-}" ] && MISSING+=("VM_USER")
[ -z "${APP_DIR:-}" ] && MISSING+=("APP_DIR")
if [ ${#MISSING[@]} -gt 0 ]; then
    echo "ERROR: Missing required variables in .deploy-config: ${MISSING[*]}"
    exit 1
fi

SSH_TARGET="${VM_USER}@${VM_HOST}"

REMOVE_VOLUMES=""
if [ "${1:-}" = "-v" ]; then
    echo "WARNING: This will remove all volumes including database data!"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted"
        exit 0
    fi
    REMOVE_VOLUMES="-v"
fi

echo "Stopping Places Tracker on ${VM_HOST}..."
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml down ${REMOVE_VOLUMES}"
echo "Places Tracker stopped"
