#!/bin/bash
set -euo pipefail

# SSH tunnel to VM MongoDB - lets local app use the VM database
# Usage: ./db-tunnel.sh        (start tunnel in background)
#        ./db-tunnel.sh stop   (kill background tunnel)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/.deploy-config"
PID_FILE="${SCRIPT_DIR}/.db-tunnel.pid"
LOCAL_PORT=27017

if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: .deploy-config not found"
    exit 1
fi
source "$CONFIG_FILE"

SSH_TARGET="${VM_USER}@${VM_HOST}"

# --- Stop command ---
if [ "${1:-}" = "stop" ]; then
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID"
            echo "Tunnel stopped (PID $PID)"
        else
            echo "Tunnel process $PID not running"
        fi
        rm -f "$PID_FILE"
    else
        echo "No tunnel PID file found"
    fi
    exit 0
fi

# --- Check local port is free ---
if lsof -i ":${LOCAL_PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Tunnel: already open on :${LOCAL_PORT}"
    exit 0
fi

# --- Start tunnel in background ---
ssh -f -N -L "127.0.0.1:${LOCAL_PORT}:127.0.0.1:27017" "$SSH_TARGET"
PID=$(lsof -i ":${LOCAL_PORT}" -sTCP:LISTEN -t 2>/dev/null | head -1)
if [ -n "$PID" ]; then
    echo "$PID" > "$PID_FILE"
    echo "Tunnel started (PID $PID): localhost:${LOCAL_PORT} -> ${VM_HOST}:27017"
else
    echo "ERROR: Tunnel failed to start"
    exit 1
fi
