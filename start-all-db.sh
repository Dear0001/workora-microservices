#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
NETWORK_NAME="workora-microservices_default"

DATABASE_SERVICES=(
  identity-db
  organization-db
  project-db
  work-db
  bug-db
  recruitment-db
  notification-db
  payment-db
)

cd "$PROJECT_ROOT"

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "Creating Docker network $NETWORK_NAME..."
  docker network create "$NETWORK_NAME" >/dev/null
fi

echo "Starting PostgreSQL containers..."
docker compose --project-name "$COMPOSE_PROJECT" up -d "${DATABASE_SERVICES[@]}"

echo "Waiting for PostgreSQL containers to become healthy..."
for service in "${DATABASE_SERVICES[@]}"; do
  container="$(
    docker compose --project-name "$COMPOSE_PROJECT" ps -q "$service"
  )"

  for attempt in {1..60}; do
    health="$(docker inspect --format '{{.State.Health.Status}}' "$container" 2>/dev/null || true)"
    if [[ "$health" == "healthy" ]]; then
      break
    fi

    if [[ "$attempt" -eq 60 ]]; then
      echo "ERROR: $service did not become healthy within 120 seconds." >&2
      exit 1
    fi
    sleep 2
  done
done

echo "All database containers are healthy."
