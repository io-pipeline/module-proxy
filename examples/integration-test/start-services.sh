#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Starting Module Proxy + Echo Service${NC}"
echo -e "${GREEN}========================================${NC}"
echo

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi

# Start services
echo -e "${YELLOW}Starting services with Docker Compose...${NC}"
docker-compose up -d

echo
echo -e "${YELLOW}Waiting for services to be healthy...${NC}"

# Wait for echo-service
echo -n "Waiting for echo-service..."
for i in {1..30}; do
    if docker-compose ps echo-service | grep -q "Up (healthy)"; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e " ${RED}✗${NC}"
        echo -e "${RED}Echo service failed to start${NC}"
        docker-compose logs echo-service
        exit 1
    fi
    echo -n "."
    sleep 2
done

# Wait for module-proxy
echo -n "Waiting for module-proxy..."
for i in {1..30}; do
    if docker-compose ps module-proxy | grep -q "Up (healthy)"; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e " ${RED}✗${NC}"
        echo -e "${RED}Module proxy failed to start${NC}"
        docker-compose logs module-proxy
        exit 1
    fi
    echo -n "."
    sleep 2
done

echo
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ Services are running!${NC}"
echo -e "${GREEN}========================================${NC}"
echo
echo -e "${BLUE}Service Endpoints:${NC}"
echo -e "  Module Proxy gRPC:  ${YELLOW}localhost:9090${NC}"
echo -e "  Module Proxy HTTP:  ${YELLOW}http://localhost:8080${NC}"
echo -e "  Echo Service gRPC:  ${YELLOW}localhost:9091${NC}"
echo
echo -e "${BLUE}Quick Tests:${NC}"
echo -e "  Health Check:       ${YELLOW}curl http://localhost:8080/q/health${NC}"
echo -e "  Metrics:            ${YELLOW}curl http://localhost:8080/q/metrics${NC}"
echo -e "  Service Info:       ${YELLOW}grpcurl -plaintext localhost:9090 com.rokkon.search.sdk.PipeStepProcessor/GetServiceRegistration${NC}"
echo
echo -e "${BLUE}Useful Commands:${NC}"
echo -e "  View logs:          ${YELLOW}docker-compose logs -f${NC}"
echo -e "  Stop services:      ${YELLOW}docker-compose down${NC}"
echo -e "  Restart:            ${YELLOW}docker-compose restart${NC}"
echo
