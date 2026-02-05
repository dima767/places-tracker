#!/bin/bash

echo "=== Starting Places Tracker infrastructure (MongoDB) ==="
docker compose up -d

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Infrastructure started successfully ==="
    echo "MongoDB is running on localhost:27017"
    echo "Data is persisted in Docker volume: mongodb_data"
    echo ""
    echo "To view logs: docker compose logs -f"
    echo "To stop: ./compose-down.sh"
else
    echo ""
    echo "=== Failed to start infrastructure ==="
    exit 1
fi
