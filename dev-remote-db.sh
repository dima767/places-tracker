#!/bin/bash
set -euo pipefail

# Ensure local dev is connected to VM MongoDB
# Safe to run multiple times - skips steps that are already done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

source "$CONFIG_FILE"
SSH_TARGET="${VM_USER}@${VM_HOST}"

# Stop local MongoDB if running
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q placestracker-mongo; then
    echo "Stopping local MongoDB..."
    docker compose -f "${SCRIPT_DIR}/docker-compose.yml" down 2>/dev/null || true
    docker compose -f "${SCRIPT_DIR}/docker-compose.standalone.yml" down 2>/dev/null || true
fi

# Check VM MongoDB is up
if ssh "$SSH_TARGET" "docker ps --format '{{.Names}}'" 2>/dev/null | grep -q placestracker-mongodb; then
    echo "VM MongoDB: running"
else
    echo "VM MongoDB: starting..."
    "${SCRIPT_DIR}/start-db-exe.sh"
fi

# Check tunnel is up
if lsof -i :27017 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Tunnel: already open"
else
    echo "Tunnel: opening..."
    "${SCRIPT_DIR}/db-tunnel.sh"
fi

echo ""
echo "Ready. Local :27017 -> VM MongoDB."
