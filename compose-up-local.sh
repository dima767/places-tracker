#!/bin/bash
# Places Tracker - Start Local Container + VM Database
# App in Docker, MongoDB on VM via SSH tunnel
#
# Usage:
#   ./compose-up-local.sh       - Start (use cached image)
#   ./compose-up-local.sh -b    - Build and start

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BUILD_FLAG=""
while getopts "b" opt; do
    case $opt in
        b)
            BUILD_FLAG="--build"
            ;;
        \?)
            echo "Usage: $0 [-b]"
            echo "  -b    Build/rebuild the application image"
            exit 1
            ;;
    esac
done

echo ""
echo "=== Starting Places Tracker (Local Container + VM Database) ==="
echo ""

# --- Ensure VM DB + tunnel are up ---
echo "Checking VM database connection..."
"${SCRIPT_DIR}/dev-remote-db.sh"
echo ""

# --- Stop local MongoDB container if running (port conflict) ---
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qE 'placestracker-mongo(db)?'; then
    echo "Stopping local MongoDB container (would conflict with tunnel)..."
    docker compose -f "${SCRIPT_DIR}/docker-compose.yml" down 2>/dev/null || true
    docker compose -f "${SCRIPT_DIR}/docker-compose.standalone.yml" down 2>/dev/null || true
    echo ""
fi

# Load environment variables
APP_HTTPS_PORT="${APP_HTTPS_PORT:-8143}"
if [ -f "${SCRIPT_DIR}/.env" ]; then
    echo "Using configuration from .env file"
    set -a
    source "${SCRIPT_DIR}/.env"
    set +a
    APP_HTTPS_PORT="${APP_HTTPS_PORT:-8143}"
else
    echo "No .env file found, using defaults (GOOGLE_MAPS_API_KEY will be empty)"
fi
echo ""

# Start app container
if [ -n "$BUILD_FLAG" ]; then
    echo "Building and starting app container..."
    docker compose -f "${SCRIPT_DIR}/docker-compose.local.yml" up -d --build
else
    echo "Starting app container..."
    docker compose -f "${SCRIPT_DIR}/docker-compose.local.yml" up -d
fi

echo ""
echo "=== Places Tracker Started ==="
echo ""
echo "  Application URL:"
echo "    HTTPS: https://localhost:${APP_HTTPS_PORT}/placestracker"
echo ""
echo "  Database: VM MongoDB via SSH tunnel (localhost:27017)"
echo ""
echo "  Useful Commands:"
echo "    View logs:          docker compose -f docker-compose.local.yml logs -f"
echo "    Stop:               ./compose-down-local.sh"
echo "    Stop + remove data: ./compose-down-local.sh -v"
echo "    Tunnel status:      lsof -i :27017"
echo "    Stop tunnel:        ./db-tunnel.sh stop"
echo ""
