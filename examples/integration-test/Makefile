.PHONY: help start stop test logs clean rebuild

# Detect docker compose command (v2: 'docker compose', v1: 'docker-compose')
DOCKER_COMPOSE := $(shell docker compose version > /dev/null 2>&1 && echo "docker compose" || echo "docker-compose")

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-15s %s\n", $$1, $$2}'

start: ## Start the integration test services
	@echo "Starting services..."
	$(DOCKER_COMPOSE) up -d
	@echo "Waiting for services to be healthy..."
	@sleep 10
	@$(DOCKER_COMPOSE) ps

stop: ## Stop the integration test services
	@echo "Stopping services..."
	$(DOCKER_COMPOSE) down

test: start ## Run integration tests
	@echo "Running integration tests..."
	@sleep 5
	./test-integration.sh

logs: ## View logs from all services
	$(DOCKER_COMPOSE) logs -f

logs-proxy: ## View logs from module-proxy only
	$(DOCKER_COMPOSE) logs -f module-proxy

logs-echo: ## View logs from echo service only
	$(DOCKER_COMPOSE) logs -f echo-service

clean: ## Clean up all resources (containers, networks, volumes)
	@echo "Cleaning up..."
	$(DOCKER_COMPOSE) down -v
	docker network rm module-proxy-integration-test 2>/dev/null || true
	@echo "Clean complete"

rebuild: clean ## Rebuild and restart everything
	@echo "Rebuilding module-proxy image..."
	cd ../.. && ./docker-build.sh
	@echo "Starting services..."
	$(MAKE) start

status: ## Show status of all services
	@echo "Service Status:"
	@$(DOCKER_COMPOSE) ps
	@echo ""
	@echo "Network Status:"
	@docker network inspect module-proxy-integration-test 2>/dev/null || echo "Network not found"

health: ## Check health of all services
	@echo "Checking echo service health..."
	@curl -s http://localhost:9091/q/health || echo "Echo service not responding"
	@echo ""
	@echo "Checking proxy health..."
	@curl -s http://localhost:39100/q/health || echo "Proxy not responding"

shell-proxy: ## Open a shell in the module-proxy container
	$(DOCKER_COMPOSE) exec module-proxy /bin/sh

shell-echo: ## Open a shell in the echo service container
	$(DOCKER_COMPOSE) exec echo-service /bin/sh
