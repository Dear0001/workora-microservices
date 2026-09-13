#!/usr/bin/env bash

set -Eeuo pipefail

WATCH_MODE=false
if [[ "${1:-}" == "--watch" ]]; then
  WATCH_MODE=true
elif [[ $# -gt 0 ]]; then
  echo "Usage: $0 [--watch]" >&2
  exit 2
fi

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-workora-microservices-2}"
NETWORK_NAME="workora-microservices_default"
STATE_FILE="$PROJECT_ROOT/.workora-service-state"

APPLICATION_SERVICES=(
  identity-service
  organization-service
  project-service
  work-service
  bug-service
  recruitment-service
  notification-service
  payment-service
  api-gateway
)

BACKEND_SERVICES=(
  identity-service
  organization-service
  project-service
  work-service
  bug-service
  recruitment-service
  notification-service
  payment-service
)

declare -A SERVICE_PORTS=(
  [identity-service]=8101
  [organization-service]=8102
  [project-service]=8103
  [work-service]=8104
  [bug-service]=8105
  [recruitment-service]=8106
  [notification-service]=8107
  [payment-service]=8108
)

cd "$PROJECT_ROOT"

source_snapshot() {
  find . \
    \( -path './target' -o -path '*/target' -o -path './.git' -o -path '*/.git' \) -prune \
    -o -type f \( -path '*/src/*' -o -name 'pom.xml' -o -name 'Dockerfile' -o -name 'docker-compose.yml' \) \
    -print0 | sort -z | xargs -0 sha256sum | sha256sum | awk '{print $1}'
}

all_services_running() {
  for service in "${APPLICATION_SERVICES[@]}"; do
    container="$(docker compose --project-name "$COMPOSE_PROJECT" ps -q "$service")"
    if [[ -z "$container" ]] || [[ "$(docker inspect --format '{{.State.Running}}' "$container")" != "true" ]]; then
      return 1
    fi
  done
  return 0
}

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "Creating Docker network $NETWORK_NAME..."
  docker network create "$NETWORK_NAME" >/dev/null
fi

CURRENT_SNAPSHOT="$(source_snapshot)"
STORED_SNAPSHOT=""
if [[ -f "$STATE_FILE" ]]; then
  STORED_SNAPSHOT="$(<"$STATE_FILE")"
fi

NEEDS_BUILD=false
if [[ "$CURRENT_SNAPSHOT" != "$STORED_SNAPSHOT" ]]; then
  echo "Source changes detected; application images need to be rebuilt."
  NEEDS_BUILD=true
elif ! all_services_running; then
  echo "Application containers are missing or stopped; they will be started."
else
  echo "All application containers are already running and no source changes were detected."
fi

if [[ "$NEEDS_BUILD" == true ]]; then
  echo "Building application containers sequentially..."
  for service in "${APPLICATION_SERVICES[@]}"; do
    built=false
    for attempt in {1..3}; do
      echo "Building $service (attempt $attempt of 3)..."
      if docker compose --project-name "$COMPOSE_PROJECT" build "$service"; then
        built=true
        break
      fi

      if [[ "$attempt" -lt 3 ]]; then
        echo "Build failed for $service. Retrying after 5 seconds..."
        sleep 5
      fi
    done

    if [[ "$built" != true ]]; then
      echo "ERROR: failed to build $service after 3 attempts." >&2
      exit 1
    fi
  done
fi

echo "Starting backend application containers..."
docker compose --project-name "$COMPOSE_PROJECT" up -d --no-build --no-deps \
  "${BACKEND_SERVICES[@]}"

for service in "${BACKEND_SERVICES[@]}"; do
  port="${SERVICE_PORTS[$service]}"
  echo "Waiting for $service on port $port..."
  for attempt in {1..60}; do
    if curl --fail --silent --show-error \
      --max-time 3 \
      "http://localhost:${port}/actuator/health" \
      >/dev/null; then
      break
    fi

    if [[ "$attempt" -eq 60 ]]; then
      echo "ERROR: $service did not become healthy within 120 seconds." >&2
      exit 1
    fi
    sleep 2
  done
done

echo "Starting API gateway..."
docker compose --project-name "$COMPOSE_PROJECT" up -d --no-build --no-deps api-gateway

echo "All application containers are starting."
echo "Swagger: http://localhost:8080/gateway/swagger-ui.html"
printf '%s\n' "$CURRENT_SNAPSHOT" > "$STATE_FILE"

if [[ "$WATCH_MODE" == true ]]; then
  snapshot() {
    source_snapshot
  }

  previous_snapshot="$(snapshot)"
  echo "Watching source files for changes. Press Ctrl+C to stop."
  while true; do
    sleep 2
    current_snapshot="$(snapshot)"
    if [[ "$current_snapshot" == "$previous_snapshot" ]]; then
      continue
    fi

    echo "Source change detected. Rebuilding application containers..."
    for service in "${APPLICATION_SERVICES[@]}"; do
      docker compose --project-name "$COMPOSE_PROJECT" build "$service"
    done
    docker compose --project-name "$COMPOSE_PROJECT" up -d --no-build --no-deps \
      "${BACKEND_SERVICES[@]}" api-gateway
    printf '%s\n' "$current_snapshot" > "$STATE_FILE"
    previous_snapshot="$current_snapshot"
    echo "Application containers restarted after source change."
  done
fi
