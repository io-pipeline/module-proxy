#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Module Proxy Integration Test Runner${NC}"
echo -e "${GREEN}========================================${NC}"
echo

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi

echo -e "${YELLOW}Building project...${NC}"
./gradlew clean build -x test

echo
echo -e "${YELLOW}Running integration tests...${NC}"
echo -e "${YELLOW}(Testcontainers will start Docker Compose automatically)${NC}"
echo

./gradlew test --info

# Check the test result
if [ $? -eq 0 ]; then
    echo
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ All integration tests passed!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo
    echo "The module-proxy successfully forwarded requests to the echo-service"
else
    echo
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ Integration tests failed${NC}"
    echo -e "${RED}========================================${NC}"
    echo
    echo "Check the logs above for details"
    exit 1
fi
