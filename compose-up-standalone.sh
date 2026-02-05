#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════
# PLACES TRACKER - Start Standalone Docker Environment
# ═══════════════════════════════════════════════════════════════════════════
#
# Starts Places Tracker with MongoDB in Docker containers with HTTPS support.
# SSL certificates are auto-generated on first start.
#
# Usage:
#   ./compose-up-standalone.sh           # Start services
#   ./compose-up-standalone.sh -b        # Rebuild and start
#   ./compose-up-standalone.sh --build   # Same as -b
#
# ═══════════════════════════════════════════════════════════════════════════

# Parse flags
BUILD_FLAG=""
if [ "$1" = "-b" ] || [ "$1" = "--build" ]; then
    BUILD_FLAG="--build"
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Building and Starting (Standalone Mode)                     ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
else
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Starting (Standalone Mode)                                  ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
fi

echo ""

# Check for .env file (optional but recommended)
if [ ! -f .env ]; then
    echo "INFO: No .env file found. Using default values."
    echo "      Create a .env file to customize:"
    echo "        GOOGLE_MAPS_API_KEY=your-api-key"
    echo "        CERT_HOSTS=your-hostname,localhost"
    echo ""
fi

# Start services
docker compose -f docker-compose.standalone.yml up -d $BUILD_FLAG

if [ $? -eq 0 ]; then
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Started Successfully                                        ║"
    echo "╠═══════════════════════════════════════════════════════════════════════════════╣"
    echo "║                                                                               ║"
    echo "║  Application URL:                                                             ║"
    echo "║    HTTPS: https://localhost:8443/placestracker                                ║"
    echo "║                                                                               ║"
    echo "║  MongoDB running on internal Docker network                                   ║"
    echo "║  SSL certificate auto-generated on first start                                ║"
    echo "║                                                                               ║"
    echo "╠═══════════════════════════════════════════════════════════════════════════════╣"
    echo "║  Useful Commands:                                                             ║"
    echo "║    View logs:       docker compose -f docker-compose.standalone.yml logs -f   ║"
    echo "║    View app logs:   docker compose -f docker-compose.standalone.yml logs -f app    ║"
    echo "║    View DB logs:    docker compose -f docker-compose.standalone.yml logs -f mongodb║"
    echo "║    Stop services:   ./compose-down-standalone.sh                              ║"
    echo "║    Rebuild & start: ./compose-up-standalone.sh --build                        ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
    echo ""
else
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Failed to Start                                             ║"
    echo "╠═══════════════════════════════════════════════════════════════════════════════╣"
    echo "║  Check logs: docker compose -f docker-compose.standalone.yml logs             ║"
    echo "╚═══════════════════════════════════════════════════════════════════════════════╝"
    echo ""
    exit 1
fi
