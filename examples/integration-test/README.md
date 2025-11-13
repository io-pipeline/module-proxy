# Module Proxy Integration Test Example

This is a **standalone integration test project** that demonstrates how the `module-proxy` service works as a sidecar with a real backend service. It uses the `echo-service` (available as a SNAPSHOT) to verify that the proxy correctly forwards gRPC requests while adding Quarkus features like health checks, metrics, and observability.

## 🎯 Purpose

This example serves two purposes:

1. **Integration Testing**: Validates that the module-proxy correctly forwards requests to backend services
2. **Tutorial**: Demonstrates how to deploy and use module-proxy in a sidecar pattern

## 📐 Architecture

```
Test Client (gRPC) --> Module Proxy (port 9090) --> Echo Service (port 9090)
                             |
                        Health/Metrics
                        (HTTP port 8080)
```

### Components

- **Echo Service**: A simple backend service that echoes back the data it receives (deployed as `1.0.0-SNAPSHOT`)
- **Module Proxy**: The Quarkus-based proxy that sits in front of the echo service
- **Integration Tests**: JUnit 5 tests using Testcontainers to verify the entire setup

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Access to the Rokkon Maven repositories (for SNAPSHOT dependencies)

### Running the Tests

```bash
# Navigate to the integration test directory
cd examples/integration-test

# Run the integration tests (Testcontainers will handle Docker Compose)
./gradlew test

# Or run with more verbose output
./gradlew test --info
```

### Manual Docker Compose Testing

If you want to start the services manually without running the tests:

```bash
# Start the services
docker-compose up -d

# Wait for services to be healthy
docker-compose ps

# Test the proxy with grpcurl
grpcurl -plaintext localhost:9090 com.rokkon.search.sdk.PipeStepProcessor/GetServiceRegistration

# Check proxy health
curl http://localhost:8080/q/health

# Check proxy metrics
curl http://localhost:8080/q/metrics

# Stop the services
docker-compose down
```

## 📝 What the Tests Verify

The integration tests verify the following functionality:

1. **Service Registration**: The proxy correctly forwards `GetServiceRegistration` calls to the echo service
2. **Data Processing**: The proxy forwards `ProcessData` requests with single documents
3. **Batch Processing**: The proxy handles multiple documents in a single request
4. **Error Handling**: The proxy gracefully handles empty or invalid requests
5. **Health Checks**: The proxy's HTTP health endpoint is accessible and working

## 🔧 Configuration

The Docker Compose setup uses these configurations:

### Echo Service
- **Port**: 9090 (gRPC)
- **Image**: `rokkon/echo-service:1.0.0-SNAPSHOT`

### Module Proxy
- **Ports**:
  - 8080 (HTTP/REST for health, metrics, etc.)
  - 9090 (gRPC for forwarding)
- **Image**: `rokkon/proxy-module:latest`
- **Environment Variables**:
  - `MODULE_HOST=echo-service` - The backend service hostname
  - `MODULE_PORT=9090` - The backend service port
  - `PROXY_PORT=8080` - The HTTP port for the proxy

## 📦 Dependencies

This project is **completely independent** from the main module-proxy build. It declares its own dependencies:

```gradle
dependencies {
    // Echo service as SNAPSHOT dependency
    testImplementation 'io.pipeline:echo-service:1.0.0-SNAPSHOT'

    // gRPC and testing dependencies
    testImplementation 'io.grpc:grpc-netty:1.68.1'
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}
```

## 🧪 Understanding the Tests

### Test 1: Service Registration
```java
@Test
void testGetServiceRegistration() {
    RegistrationRequest request = RegistrationRequest.newBuilder().build();
    RegistrationResponse response = asyncStub.getServiceRegistration(request)
        .await().atMost(Duration.ofSeconds(10));

    assertThat(response.getModuleName()).isEqualTo("echo-service");
}
```

This test verifies that the proxy correctly forwards service registration requests to the echo service and returns the response.

### Test 2: Data Processing
```java
@Test
void testProcessData() {
    ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
        .addDocuments(document)
        .build();

    ModuleProcessResponse response = asyncStub.processData(request)
        .await().atMost(Duration.ofSeconds(10));

    assertThat(response.getSuccess()).isTrue();
}
```

This test sends a document through the proxy to the echo service and verifies it comes back correctly.

## 🔍 Debugging

### View Logs

```bash
# View all logs
docker-compose logs

# View proxy logs only
docker-compose logs module-proxy

# View echo service logs only
docker-compose logs echo-service

# Follow logs in real-time
docker-compose logs -f
```

### Test with grpcurl

```bash
# List available services
grpcurl -plaintext localhost:9090 list

# Get service registration
grpcurl -plaintext localhost:9090 com.rokkon.search.sdk.PipeStepProcessor/GetServiceRegistration

# Process data (you'll need to create a JSON request file)
grpcurl -plaintext -d @ localhost:9090 com.rokkon.search.sdk.PipeStepProcessor/ProcessData < request.json
```

### Check Health and Metrics

```bash
# Health check (liveness)
curl http://localhost:8080/q/health/live

# Health check (readiness)
curl http://localhost:8080/q/health/ready

# Prometheus metrics
curl http://localhost:8080/q/metrics
```

## 🎓 Using this as a Template

You can use this example as a template for testing your own modules:

1. **Replace the echo-service** with your own module implementation
2. **Update the dependency** in `build.gradle` to point to your module
3. **Modify the tests** to verify your module's specific functionality
4. **Update docker-compose.yml** with your module's configuration

### Example: Testing Your Own Module

```gradle
dependencies {
    // Replace echo-service with your module
    testImplementation 'io.pipeline:your-module:1.0.0-SNAPSHOT'
}
```

```yaml
# docker-compose.yml
services:
  your-module:
    image: rokkon/your-module:1.0.0-SNAPSHOT
    # ... rest of configuration
```

## 🏗️ Project Structure

```
examples/integration-test/
├── build.gradle                    # Gradle build configuration (independent)
├── settings.gradle                 # Gradle settings
├── docker-compose.yml              # Docker Compose setup
├── README.md                       # This file
├── gradlew                        # Gradle wrapper (Linux/Mac)
├── gradlew.bat                    # Gradle wrapper (Windows)
└── src/
    └── test/
        └── java/
            └── io/pipeline/examples/integration/
                └── ModuleProxyIntegrationTest.java
```

## 💡 Benefits of This Approach

1. **Separation of Concerns**: Integration tests are separate from the main build
2. **Real Dependencies**: Uses actual SNAPSHOT versions from Maven
3. **Complete Isolation**: Can be run independently without building module-proxy
4. **Realistic Testing**: Tests the actual Docker images that would be deployed
5. **Easy Debugging**: Can start services manually with Docker Compose

## 🔗 Related Documentation

- [Module Proxy README](../../README.md) - Main module-proxy documentation
- [Echo Service Documentation](https://git.rokkon.com/io-pipeline/echo-service) - Echo service details
- [Testcontainers Documentation](https://www.testcontainers.org/) - Testcontainers framework

## 📞 Support

If you encounter issues:

1. Check that Docker is running: `docker ps`
2. Verify network connectivity: `docker network ls`
3. Check service health: `docker-compose ps`
4. View logs: `docker-compose logs`

## 📄 License

Apache License 2.0 - See [LICENSE](../../LICENSE) file
