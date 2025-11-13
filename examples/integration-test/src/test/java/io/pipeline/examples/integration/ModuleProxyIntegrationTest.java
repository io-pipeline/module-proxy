package io.pipeline.examples.integration;

import com.rokkon.search.proto.ModuleProcessRequest;
import com.rokkon.search.proto.ModuleProcessResponse;
import com.rokkon.search.proto.MutinyPipeStepProcessorGrpc;
import com.rokkon.search.proto.RegistrationRequest;
import com.rokkon.search.proto.RegistrationResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the module-proxy service using the echo-service as a backend.
 *
 * This test demonstrates how the module-proxy works as a sidecar, forwarding gRPC
 * requests to a backend service while adding Quarkus features like health checks,
 * metrics, and observability.
 *
 * Test Architecture:
 * <pre>
 * Test Client (gRPC) --> Module Proxy (port 9090) --> Echo Service (port 9090)
 *                              |
 *                         Health/Metrics
 *                         (HTTP port 8080)
 * </pre>
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModuleProxyIntegrationTest {

    private static final String ECHO_SERVICE = "echo-service";
    private static final String MODULE_PROXY = "module-proxy";
    private static final int GRPC_PORT = 9090;
    private static final int HTTP_PORT = 8080;

    @Container
    static ComposeContainer environment = new ComposeContainer(
            new File("docker-compose.yml"))
            .withExposedService(ECHO_SERVICE, GRPC_PORT,
                    Wait.forLogMessage(".*gRPC server started.*", 1)
                            .withStartupTimeout(Duration.ofSeconds(60)))
            .withExposedService(MODULE_PROXY, HTTP_PORT,
                    Wait.forHttp("/q/health/ready")
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofSeconds(90)))
            .withExposedService(MODULE_PROXY, GRPC_PORT,
                    Wait.forListeningPort()
                            .withStartupTimeout(Duration.ofSeconds(90)));

    private static ManagedChannel channel;
    private static MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub asyncStub;

    @BeforeAll
    static void setUp() {
        // Get the proxy's gRPC port
        String proxyHost = environment.getServiceHost(MODULE_PROXY, GRPC_PORT);
        Integer proxyPort = environment.getServicePort(MODULE_PROXY, GRPC_PORT);

        System.out.println("Connecting to module-proxy at " + proxyHost + ":" + proxyPort);

        // Create gRPC channel to the proxy
        channel = ManagedChannelBuilder
                .forAddress(proxyHost, proxyPort)
                .usePlaintext()
                .build();

        // Create async stub for non-blocking calls
        asyncStub = MutinyPipeStepProcessorGrpc.newMutinyStub(channel);
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should get service registration from echo service through proxy")
    void testGetServiceRegistration() {
        // Arrange
        RegistrationRequest request = RegistrationRequest.newBuilder().build();

        // Act
        RegistrationResponse response = asyncStub.getServiceRegistration(request)
                .await()
                .atMost(Duration.ofSeconds(10));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getModuleName()).isEqualTo("echo-service");
        assertThat(response.getVersion()).isNotEmpty();

        System.out.println("✓ Service Registration:");
        System.out.println("  Module Name: " + response.getModuleName());
        System.out.println("  Version: " + response.getVersion());
        System.out.println("  Description: " + response.getDescription());
    }

    @Test
    @Order(2)
    @DisplayName("Should process data through proxy to echo service")
    void testProcessData() {
        // Arrange
        String testData = "Hello from integration test!";
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setIndexName("test-index")
                .setCollectionName("test-collection")
                .addDocuments(com.rokkon.search.proto.Document.newBuilder()
                        .setDocId("test-doc-1")
                        .putFields("content", com.rokkon.search.proto.Field.newBuilder()
                                .setTextValue(testData)
                                .build())
                        .build())
                .build();

        // Act
        ModuleProcessResponse response = asyncStub.processData(request)
                .await()
                .atMost(Duration.ofSeconds(10));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getProcessedDocumentsList()).hasSize(1);

        // Echo service should return the same document
        assertThat(response.getProcessedDocuments(0).getDocId()).isEqualTo("test-doc-1");
        assertThat(response.getProcessedDocuments(0).getFieldsMap())
                .containsKey("content");

        System.out.println("✓ Data Processing:");
        System.out.println("  Success: " + response.getSuccess());
        System.out.println("  Processed Documents: " + response.getProcessedDocumentsCount());
        System.out.println("  Logs: " + response.getProcessorLogsList());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle multiple document processing")
    void testProcessMultipleDocuments() {
        // Arrange
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setIndexName("test-index")
                .setCollectionName("test-collection")
                .addDocuments(createDocument("doc-1", "Content 1"))
                .addDocuments(createDocument("doc-2", "Content 2"))
                .addDocuments(createDocument("doc-3", "Content 3"))
                .build();

        // Act
        ModuleProcessResponse response = asyncStub.processData(request)
                .await()
                .atMost(Duration.ofSeconds(10));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getProcessedDocumentsList()).hasSize(3);

        System.out.println("✓ Multiple Document Processing:");
        System.out.println("  Success: " + response.getSuccess());
        System.out.println("  Processed Documents: " + response.getProcessedDocumentsCount());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle empty request gracefully")
    void testEmptyRequest() {
        // Arrange
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setIndexName("test-index")
                .setCollectionName("test-collection")
                .build();

        // Act
        ModuleProcessResponse response = asyncStub.processData(request)
                .await()
                .atMost(Duration.ofSeconds(10));

        // Assert
        assertThat(response).isNotNull();
        // Echo service should handle empty requests gracefully

        System.out.println("✓ Empty Request Handling:");
        System.out.println("  Success: " + response.getSuccess());
        System.out.println("  Processed Documents: " + response.getProcessedDocumentsCount());
    }

    @Test
    @Order(5)
    @DisplayName("Proxy health endpoint should be accessible")
    void testProxyHealthEndpoint() {
        // This test verifies that the proxy's HTTP health endpoint is working
        // Testcontainers already verified this during startup with the health check,
        // but we can make it explicit in the test output

        String proxyHost = environment.getServiceHost(MODULE_PROXY, HTTP_PORT);
        Integer proxyPort = environment.getServicePort(MODULE_PROXY, HTTP_PORT);

        System.out.println("✓ Proxy Health Endpoint:");
        System.out.println("  URL: http://" + proxyHost + ":" + proxyPort + "/q/health");
        System.out.println("  Status: Available (verified by Testcontainers)");
    }

    /**
     * Helper method to create a document for testing
     */
    private com.rokkon.search.proto.Document createDocument(String docId, String content) {
        return com.rokkon.search.proto.Document.newBuilder()
                .setDocId(docId)
                .putFields("content", com.rokkon.search.proto.Field.newBuilder()
                        .setTextValue(content)
                        .build())
                .build();
    }
}
