#!/bin/bash
set -euo pipefail

# Places Tracker deployment to exe.dev VM
# Deploys the application via Docker Compose on the remote VM

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"

# --- Load config ---
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
    echo "Run: cp .deploy-config.example .deploy-config and fill in your values"
    exit 1
fi
source "$CONFIG_FILE"

# --- Validate required vars ---
MISSING=()
[ -z "${VM_HOST:-}" ] && MISSING+=("VM_HOST")
[ -z "${VM_USER:-}" ] && MISSING+=("VM_USER")
[ -z "${APP_DIR:-}" ] && MISSING+=("APP_DIR")
[ -z "${REPO_URL:-}" ] && MISSING+=("REPO_URL")

if [ ${#MISSING[@]} -gt 0 ]; then
    echo "ERROR: Missing required variables in .deploy-config: ${MISSING[*]}"
    exit 1
fi

ENV_FILE="${SCRIPT_DIR}/.env.prod"
if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: .env.prod not found"
    echo "Run: cp .env.prod.example .env.prod and fill in your values"
    exit 1
fi

SSH_TARGET="${VM_USER}@${VM_HOST}"

# --- Check SSH connectivity ---
echo "Checking SSH connectivity to ${SSH_TARGET}..."
if ! ssh -o ConnectTimeout=5 "$SSH_TARGET" echo ok > /dev/null 2>&1; then
    echo "ERROR: Cannot connect to ${SSH_TARGET}"
    echo "Make sure the VM is running and your SSH key is configured"
    exit 1
fi
echo "SSH connection OK"

# --- Clone or pull repo on VM ---
echo "Setting up repository on VM..."
ssh "$SSH_TARGET" bash -s -- "$APP_DIR" "$REPO_URL" << 'REMOTE_REPO'
APP_DIR="$1"
REPO_URL="$2"

if [ -d "$APP_DIR/.git" ]; then
    echo "Repository exists, pulling latest..."
    cd "$APP_DIR"
    git fetch origin
    git reset --hard origin/$(git rev-parse --abbrev-ref HEAD)
else
    echo "Cloning repository..."
    git clone "$REPO_URL" "$APP_DIR"
fi
REMOTE_REPO

# --- SCP .env.prod as .env ---
echo "Copying environment file to VM..."
scp "$ENV_FILE" "${SSH_TARGET}:${APP_DIR}/.env"
ssh "$SSH_TARGET" "chmod 600 '${APP_DIR}/.env'"

# --- Build and start containers ---
echo "Building and starting containers on VM..."
ssh "$SSH_TARGET" "cd '${APP_DIR}' && docker compose -f docker-compose.prod.yml up -d --build"

# --- Set exe.dev proxy port ---
echo "Configuring exe.dev proxy..."
VM_NAME="${VM_HOST%%.*}"
ssh exe.dev share port "$VM_NAME" 8080 || echo "WARNING: Failed to set exe.dev proxy port (may already be configured)"

# --- Done ---
echo ""
echo "=== Deployment complete ==="
echo "Places Tracker is running at: https://${VM_HOST}/placestracker/"
echo ""
echo "Useful commands:"
echo "  ./status-exe.sh  - Check status"
echo "  ./logs-exe.sh    - View logs"
echo "  ./stop-exe.sh    - Stop services"
