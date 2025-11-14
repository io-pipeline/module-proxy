#!/bin/bash

echo "Cleaning up module-proxy integration test resources..."

# Detect docker compose command (v2: 'docker compose', v1: 'docker-compose')
DOCKER_COMPOSE="docker compose"
if ! docker compose version &> /dev/null; then
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
    else
        echo "Error: docker compose is not installed"
        exit 1
    fi
fi

# Stop and remove containers
echo "Stopping containers..."
$DOCKER_COMPOSE down

# Remove volumes (optional, ask user)
read -p "Remove volumes? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing volumes..."
    $DOCKER_COMPOSE down -v
fi

# Remove network if it exists
echo "Removing network..."
docker network rm module-proxy-integration-test 2>/dev/null || true

echo "Cleanup complete!"
