#!/bin/bash
set -euo pipefail

# Start only the MongoDB container on exe.dev VM
# Use this when you want the shared DB running but not the app

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
    exit 1
fi
source "$CONFIG_FILE"

SSH_TARGET="${VM_USER}@${VM_HOST}"

echo "Starting MongoDB on ${VM_HOST}..."
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml up -d mongodb"
echo "MongoDB started"
echo ""
echo "Connect locally via: ./db-tunnel.sh"
