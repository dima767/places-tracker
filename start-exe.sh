#!/bin/bash
set -euo pipefail

# Places Tracker start on exe.dev VM (no rebuild)
# Starts existing containers without rebuilding images

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

echo "Starting Places Tracker on ${VM_HOST} (no rebuild)..."
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml up -d"
echo "Places Tracker started"
