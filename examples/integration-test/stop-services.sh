#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Stopping services...${NC}"
docker-compose down

echo -e "${GREEN}✓ Services stopped${NC}"
echo

# Optionally clean up volumes
read -p "Remove volumes? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Removing volumes...${NC}"
    docker-compose down -v
    echo -e "${GREEN}✓ Volumes removed${NC}"
fi
