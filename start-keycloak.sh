#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
NETWORK_NAME="workora-microservices_default"

cd "$PROJECT_ROOT"

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "Creating Docker network $NETWORK_NAME..."
  docker network create "$NETWORK_NAME" >/dev/null
fi

echo "Starting Keycloak..."
docker compose --project-name "$COMPOSE_PROJECT" up -d keycloak

echo "Waiting for Keycloak discovery..."
for attempt in {1..60}; do
  if curl --fail --silent --show-error \
    --max-time 3 \
    "http://localhost:8180/realms/workora/.well-known/openid-configuration" \
    >/dev/null; then
    echo "Keycloak is ready at http://localhost:8180."
    exit 0
  fi
  sleep 2
done

echo "ERROR: Keycloak did not become ready within 120 seconds." >&2
exit 1
