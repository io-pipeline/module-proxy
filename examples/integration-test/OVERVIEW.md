# Integration Test Overview

## What Was Created

This integration test example demonstrates a **complete, standalone testing setup** for the module-proxy service. Here's what's included:

### 📁 Project Structure

```
examples/integration-test/
├── build.gradle                          # Independent Gradle build
├── settings.gradle                       # Standalone project settings
├── docker-compose.yml                    # Services orchestration
├── README.md                             # Complete tutorial
├── .gitignore                           # Git ignore rules
├── gradlew, gradlew.bat                 # Gradle wrappers
├── gradle/                              # Gradle wrapper files
├── run-tests.sh                         # Quick test runner
├── start-services.sh                    # Manual service starter
├── stop-services.sh                     # Service cleanup
└── src/test/java/                       # Integration tests
    └── io/pipeline/examples/integration/
        └── ModuleProxyIntegrationTest.java

```

### 🎯 Key Features

1. **100% Independent**: Completely separate from module-proxy build
2. **Real Dependencies**: Uses echo-service:1.0.0-SNAPSHOT from Maven
3. **Automated Testing**: Testcontainers handles Docker orchestration
4. **Manual Testing**: Can also run services with Docker Compose directly
5. **Production-like**: Tests actual Docker images, not mocks

### 🔧 How It Works

```
┌─────────────────┐
│ Integration Test│
│   (JUnit 5)     │
└────────┬────────┘
         │ gRPC calls
         ▼
┌─────────────────┐      ┌─────────────────┐
│  Module Proxy   │─────▶│  Echo Service   │
│  (port 9090)    │ gRPC │  (port 9090)    │
└─────────────────┘      └─────────────────┘
         │
         │ HTTP
         ▼
┌─────────────────┐
│ Health/Metrics  │
│  (port 8080)    │
└─────────────────┘
```

### 📝 Test Coverage

The integration tests verify:

✓ Service registration forwarding
✓ Single document processing
✓ Batch document processing
✓ Empty request handling
✓ Health endpoint availability
✓ Error handling

### 🚀 Usage Options

#### Option 1: Automated Testing (Recommended)
```bash
./run-tests.sh
```
Testcontainers automatically starts/stops services.

#### Option 2: Manual Testing
```bash
# Start services
./start-services.sh

# Test with grpcurl
grpcurl -plaintext localhost:9090 com.rokkon.search.sdk.PipeStepProcessor/GetServiceRegistration

# Check health
curl http://localhost:8080/q/health

# Stop services
./stop-services.sh
```

### 🎓 Use as Template

To test your own module:

1. Replace `echo-service:1.0.0-SNAPSHOT` in `build.gradle`
2. Update `docker-compose.yml` with your module image
3. Modify tests to verify your module's behavior
4. Run `./run-tests.sh`

### 📦 Dependencies

Uses these key SNAPSHOT dependencies:
- `io.pipeline:echo-service:1.0.0-SNAPSHOT` - Backend test module
- `io.pipeline:grpc-stubs:1.0.0-SNAPSHOT` - gRPC protocol definitions
- `io.pipeline:pipeline-api:1.0.0-SNAPSHOT` - Pipeline API types

Plus standard testing tools:
- JUnit 5
- Testcontainers
- AssertJ
- gRPC Java libraries

### 🌟 Benefits

1. **Isolated Testing**: No impact on main build
2. **Real Environment**: Tests actual Docker deployment
3. **Easy Debugging**: Can start/stop services manually
4. **Continuous Integration**: Ready for CI/CD pipelines
5. **Documentation**: Serves as usage tutorial

### 🔗 Next Steps

- Read the [detailed README](README.md)
- Run the tests: `./run-tests.sh`
- Explore the [test code](src/test/java/io/pipeline/examples/integration/ModuleProxyIntegrationTest.java)
- Adapt for your own modules
