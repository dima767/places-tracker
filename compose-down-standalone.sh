#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════════
# PLACES TRACKER - Stop Standalone Docker Environment
# ═══════════════════════════════════════════════════════════════════════════════
#
# Stops Places Tracker Docker containers.
#
# Usage:
#   ./compose-down-standalone.sh      # Stop services (keep data)
#   ./compose-down-standalone.sh -v   # Stop and remove all volumes (data loss!)
#
# ═══════════════════════════════════════════════════════════════════════════════

# Parse flags
VOLUMES_FLAG=""
if [ "$1" = "-v" ]; then
    VOLUMES_FLAG="-v"
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Stopping (with volume removal)                              ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
else
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Stopping                                                    ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
fi

echo ""

docker compose -f docker-compose.standalone.yml down $VOLUMES_FLAG

if [ $? -eq 0 ]; then
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Stopped Successfully                                        ║"
    echo "╠═══════════════════════════════════════════════════════════════════════════════╣"
    if [ -n "$VOLUMES_FLAG" ]; then
        echo "║  All data volumes have been removed:                                          ║"
        echo "║    - mongodb_data (database)                                                  ║"
        echo "║    - mongodb_config (config)                                                  ║"
        echo "║    - app_logs (application logs)                                              ║"
        echo "║    - app_certs (SSL certificates)                                             ║"
    else
        echo "║  Data preserved in volumes:                                                   ║"
        echo "║    - mongodb_data (database)                                                  ║"
        echo "║    - mongodb_config (config)                                                  ║"
        echo "║    - app_logs (application logs)                                              ║"
        echo "║    - app_certs (SSL certificates)                                             ║"
        echo "║                                                                               ║"
        echo "║  To remove all data: ./compose-down-standalone.sh -v                          ║"
    fi
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
    echo ""
else
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Failed to Stop                                              ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
    echo ""
    exit 1
fi
