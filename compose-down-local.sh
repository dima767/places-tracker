#!/bin/bash
# Places Tracker - Stop Local Container (VM DB stays running)
#
# Usage:
#   ./compose-down-local.sh      - Stop (preserve certs/logs)
#   ./compose-down-local.sh -v   - Stop and remove volumes

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

VOLUMES_FLAG=""
if [ "${1:-}" = "-v" ]; then
    VOLUMES_FLAG="-v"
    echo ""
    echo "=== Stopping Places Tracker App (with volume removal) ==="
else
    echo ""
    echo "=== Stopping Places Tracker App ==="
fi
echo ""

docker compose -f "${SCRIPT_DIR}/docker-compose.local.yml" down $VOLUMES_FLAG

echo ""
echo "  Places Tracker app stopped."
echo ""
echo "  Note: VM database and SSH tunnel are still running."
echo "  To stop tunnel: ./db-tunnel.sh stop"
echo ""
