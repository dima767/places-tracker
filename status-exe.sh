#!/bin/bash
set -euo pipefail

# Places Tracker status check on exe.dev VM
# Shows container status and recent log output

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
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

echo "=== Container Status ==="
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml ps"

echo ""
echo "=== Recent Logs (last 30 lines) ==="
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml logs --tail=30"
