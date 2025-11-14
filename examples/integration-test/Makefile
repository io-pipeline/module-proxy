.PHONY: help start stop test logs clean rebuild

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-15s %s\n", $$1, $$2}'

start: ## Start the integration test services
	@echo "Starting services..."
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 10
	@docker-compose ps

stop: ## Stop the integration test services
	@echo "Stopping services..."
	docker-compose down

test: start ## Run integration tests
	@echo "Running integration tests..."
	@sleep 5
	./test-integration.sh

logs: ## View logs from all services
	docker-compose logs -f

logs-proxy: ## View logs from module-proxy only
	docker-compose logs -f module-proxy

logs-echo: ## View logs from echo service only
	docker-compose logs -f echo-service

clean: ## Clean up all resources (containers, networks, volumes)
	@echo "Cleaning up..."
	docker-compose down -v
	docker network rm module-proxy-integration-test 2>/dev/null || true
	@echo "Clean complete"

rebuild: clean ## Rebuild and restart everything
	@echo "Rebuilding module-proxy image..."
	cd ../.. && ./docker-build.sh
	@echo "Starting services..."
	$(MAKE) start

status: ## Show status of all services
	@echo "Service Status:"
	@docker-compose ps
	@echo ""
	@echo "Network Status:"
	@docker network inspect module-proxy-integration-test 2>/dev/null || echo "Network not found"

health: ## Check health of all services
	@echo "Checking echo service health..."
	@grpcurl -plaintext -d '{"service":""}' localhost:9091 grpc.health.v1.Health/Check || echo "Echo service not responding"
	@echo ""
	@echo "Checking proxy health..."
	@curl -s http://localhost:39100/q/health || echo "Proxy not responding"

shell-proxy: ## Open a shell in the module-proxy container
	docker-compose exec module-proxy /bin/sh

shell-echo: ## Open a shell in the echo service container
	docker-compose exec echo-service /bin/sh
