#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
STATE_FILE="$PROJECT_ROOT/.workora-service-state"

cd "$PROJECT_ROOT"

echo "Removing all Workora Compose containers, images, volumes, and networks..."
docker compose \
  --project-name "$COMPOSE_PROJECT" \
  down \
  --volumes \
  --remove-orphans \
  --rmi all

if [[ -f "$STATE_FILE" ]]; then
  rm -f "$STATE_FILE"
fi

echo "All Workora Compose resources have been removed."
