#!/bin/bash

# Parse flags
VOLUMES_FLAG=""
if [ "$1" = "-v" ]; then
    VOLUMES_FLAG="-v"
    echo "=== Stopping Places Tracker infrastructure (with volume removal) ==="
else
    echo "=== Stopping Places Tracker infrastructure ==="
fi

docker compose down $VOLUMES_FLAG

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Infrastructure stopped successfully ==="
    if [ -n "$VOLUMES_FLAG" ]; then
        echo "MongoDB data volume has been removed"
    else
        echo "Note: MongoDB data is preserved in volume 'mongodb_data'"
        echo "To remove data volume: ./compose-down.sh -v"
    fi
else
    echo ""
    echo "=== Failed to stop infrastructure ==="
    exit 1
fi
