#!/bin/bash
set -euo pipefail

# Places Tracker log viewer on exe.dev VM
# Usage: ./logs-exe.sh [service]
# service: app, mongodb (default: all services)

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
SERVICE="${1:-}"
if [ -n "$SERVICE" ]; then
    case "$SERVICE" in
        app|mongodb) ;;
        *) echo "ERROR: Unknown service '$SERVICE'. Use: app, mongodb"; exit 1 ;;
    esac
fi

echo "Following logs on ${VM_HOST}... (Ctrl+C to stop)"
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml logs -f --tail=100 ${SERVICE}"
