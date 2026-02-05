#!/bin/bash
set -e

# ═══════════════════════════════════════════════════════════════════════════
# PLACES TRACKER - Docker Entrypoint Script
# ═══════════════════════════════════════════════════════════════════════════
#
# Generates SSL certificates if they don't exist, then starts the application.
#
# ═══════════════════════════════════════════════════════════════════════════

CERTS_DIR="/app/certs"
KEYSTORE_PATH="${CERTS_DIR}/placestracker-keystore.p12"
CERT_PASSWORD="${CERT_PASSWORD:-placestracker-dev-cert}"
CERT_ALIAS="${CERT_ALIAS:-placestracker-local}"
CERT_VALIDITY="${CERT_VALIDITY:-3650}"
CERT_HOSTS="${CERT_HOSTS:-localhost,placestracker.local}"
CERT_IPS="${CERT_IPS:-127.0.0.1,0.0.0.0}"

# Use first hostname as CN
CERT_CN=$(echo "$CERT_HOSTS" | cut -d',' -f1)

# Generate SSL certificate if it doesn't exist
if [ ! -f "${KEYSTORE_PATH}" ]; then
    echo ""
    echo "╔════════════════════════════════════════════════════╗"
    echo "║  PLACES TRACKER - Generating SSL Certificate       ║"
    echo "╚════════════════════════════════════════════════════╝"
    echo ""

    # Build SAN extension string
    SAN_PARTS=""
    IFS=',' read -ra HOST_ARRAY <<< "$CERT_HOSTS"
    for host in "${HOST_ARRAY[@]}"; do
        host=$(echo "$host" | xargs)
        if [ -n "$SAN_PARTS" ]; then
            SAN_PARTS="${SAN_PARTS},"
        fi
        SAN_PARTS="${SAN_PARTS}DNS:${host}"
    done

    IFS=',' read -ra IP_ARRAY <<< "$CERT_IPS"
    for ip in "${IP_ARRAY[@]}"; do
        ip=$(echo "$ip" | xargs)
        SAN_PARTS="${SAN_PARTS},IP:${ip}"
    done

    echo "Generating certificate with:"
    echo "  CN:   ${CERT_CN}"
    echo "  SAN:  ${SAN_PARTS}"
    echo ""

    keytool -genkeypair \
        -alias "${CERT_ALIAS}" \
        -keyalg RSA \
        -keysize 2048 \
        -storetype PKCS12 \
        -keystore "${KEYSTORE_PATH}" \
        -storepass "${CERT_PASSWORD}" \
        -validity "${CERT_VALIDITY}" \
        -dname "CN=${CERT_CN}, OU=Places Tracker Docker, O=Development, L=Local, ST=Dev, C=US" \
        -ext "SAN=${SAN_PARTS}"

    echo ""
    echo "Certificate generated successfully!"
    echo ""
else
    echo "Using existing SSL certificate from ${KEYSTORE_PATH}"
fi

# Start the application
echo ""
echo "╔════════════════════════════════════════════════════╗"
echo "║  PLACES TRACKER - Starting Application             ║"
echo "╚════════════════════════════════════════════════════╝"
echo ""

exec java ${JAVA_OPTS:--Xms512m -Xmx1g} -jar app.jar \
    --spring.profiles.active=docker \
    "$@"
