#!/bin/bash

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "Module-Proxy Integration Test"
echo "======================================"
echo ""

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed"
    exit 1
fi

# Check if grpcurl is available
if ! command -v grpcurl &> /dev/null; then
    print_error "grpcurl is not installed. Please install it: https://github.com/fullstorydev/grpcurl#installation"
    exit 1
fi

print_info "Starting services with docker-compose..."
docker-compose up -d

echo ""
print_info "Waiting for services to be healthy..."

# Wait for echo service to be healthy
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if docker-compose ps echo-service | grep -q "healthy"; then
        print_success "Echo service is healthy"
        break
    fi

    sleep 5
    WAITED=$((WAITED + 5))
    echo -n "."
done

if [ $WAITED -ge $MAX_WAIT ]; then
    print_error "Echo service failed to become healthy"
    docker-compose logs echo-service
    exit 1
fi

echo ""

# Wait for proxy to be healthy
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if docker-compose ps module-proxy | grep -q "healthy"; then
        print_success "Module-proxy is healthy"
        break
    fi

    sleep 5
    WAITED=$((WAITED + 5))
    echo -n "."
done

if [ $WAITED -ge $MAX_WAIT ]; then
    print_error "Module-proxy failed to become healthy"
    docker-compose logs module-proxy
    exit 1
fi

echo ""
echo ""
echo "======================================"
echo "Running Integration Tests"
echo "======================================"
echo ""

# Test 1: Check if we can list services via the proxy
print_info "Test 1: Listing gRPC services via proxy..."
if grpcurl -plaintext localhost:39100 list > /dev/null 2>&1; then
    print_success "Successfully listed services via proxy"
else
    print_error "Failed to list services via proxy"
    exit 1
fi

# Test 2: Get service registration via proxy
print_info "Test 2: Getting service registration via proxy..."
REGISTRATION=$(grpcurl -plaintext localhost:39100 com.rokkon.search.sdk.PipeStepProcessor/GetServiceRegistration)
if echo "$REGISTRATION" | grep -q "moduleName"; then
    print_success "Successfully retrieved service registration"
    echo "$REGISTRATION" | grep -E "moduleName|version|description" | sed 's/^/  /'
else
    print_error "Failed to get service registration"
    exit 1
fi

# Test 3: Process data via proxy (echo test)
print_info "Test 3: Processing data through proxy to echo service..."
TEST_REQUEST='{"documentBatch": {"documents": [{"id": "test-1", "body": "Hello from integration test"}]}}'
RESPONSE=$(echo "$TEST_REQUEST" | grpcurl -plaintext -d @ localhost:39100 com.rokkon.search.sdk.PipeStepProcessor/ProcessData)
if echo "$RESPONSE" | grep -q "success"; then
    print_success "Successfully processed data through proxy"
    echo "$RESPONSE" | grep -E "success|processorLogs" | head -5 | sed 's/^/  /'
else
    print_error "Failed to process data through proxy"
    exit 1
fi

# Test 4: Check proxy health endpoint
print_info "Test 4: Checking proxy health endpoint..."
HEALTH_STATUS=$(curl -s http://localhost:39100/q/health/ready)
if echo "$HEALTH_STATUS" | grep -q "UP"; then
    print_success "Proxy health check passed"
else
    print_error "Proxy health check failed"
    echo "$HEALTH_STATUS"
    exit 1
fi

# Test 5: Check metrics endpoint
print_info "Test 5: Checking proxy metrics endpoint..."
if curl -s http://localhost:39100/q/metrics | grep -q "proxy_requests_processed"; then
    print_success "Metrics endpoint is accessible"
else
    print_error "Metrics endpoint check failed"
    exit 1
fi

echo ""
echo "======================================"
echo -e "${GREEN}All Integration Tests Passed!${NC}"
echo "======================================"
echo ""
print_info "To view logs, run: docker-compose logs -f"
print_info "To stop services, run: docker-compose down"
echo ""
