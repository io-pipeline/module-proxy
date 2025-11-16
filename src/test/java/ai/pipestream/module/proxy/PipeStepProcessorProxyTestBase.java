package ai.pipestream.module.proxy;

import ai.pipestream.data.module.MutinyPipeStepProcessorGrpc;
import ai.pipestream.data.module.PipeStepProcessor;
import ai.pipestream.data.module.ModuleProcessRequest;
import ai.pipestream.data.module.ModuleProcessResponse;
import ai.pipestream.data.module.RegistrationRequest;
import ai.pipestream.data.module.ServiceRegistrationMetadata;
import ai.pipestream.data.v1.PipeDoc;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Base test class for PipeStepProcessorProxy testing.
 * This abstract class can be extended by both unit tests and integration tests.
 * <p>
 * The class provides a comprehensive set of tests for the PipeStepProcessor interface:
 * <p>
 * - testProcessData: Tests the basic processing functionality with a valid document
 * - testTestProcessData: Tests the test processing functionality
 * - testGetServiceRegistration: Tests retrieving service registration information
 * - testProcessDataError: Tests handling of backend exceptions during processing
 * - testBackendFailureResponse: Tests handling of failure responses from the backend
 * - testGetServiceRegistrationError: Tests handling of backend exceptions during registration
 * <p>
 * Each test mocks the backend client's behavior and verifies that the proxy correctly
 * handles the response or error. This approach allows testing the proxy's error handling
 * and response processing without requiring a real backend service.
 */
public abstract class PipeStepProcessorProxyTestBase {

    protected abstract PipeStepProcessor getProxyProcessor();
    protected abstract MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub getMockedBackendClient();

    // No need for a setup method in the base class
    // Each subclass should handle its own setup

    @Test
    void testProcessData() {
        // Create test document
        PipeDoc document = PipeDoc.newBuilder()
                .setDocId(UUID.randomUUID().toString())
                .build();

        // Create request with metadata
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(document)
                .build();

        // Mock the backend response
        ModuleProcessResponse backendResponse = ModuleProcessResponse.newBuilder()
                .setSuccess(true)
                .setOutputDoc(document)
                .addProcessorLogs("Backend: Document processed successfully")
                .build();

        when(getMockedBackendClient().processData(any(ModuleProcessRequest.class)))
                .thenReturn(Uni.createFrom().item(backendResponse));

        // Process and verify
        UniAssertSubscriber<ModuleProcessResponse> subscriber = getProxyProcessor()
                .processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ModuleProcessResponse response = subscriber.awaitItem().getItem();

        assertThat(response, notNullValue());
        assertThat(response.getSuccess(), is(true));
        assertThat(response.hasOutputDoc(), is(true));
        assertThat(response.getOutputDoc().getDocId(), is(equalTo(document.getDocId())));
        assertThat(response.getProcessorLogsList(), hasItem(containsString("Backend: Document processed successfully")));
    }

    @Test
    void testTestProcessData() {
        // Create test document
        PipeDoc document = PipeDoc.newBuilder()
                .setDocId(UUID.randomUUID().toString())
                .build();

        // Create request
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(document)
                .build();

        // Mock the backend response
        ModuleProcessResponse backendResponse = ModuleProcessResponse.newBuilder()
                .setSuccess(true)
                .setOutputDoc(document)
                .addProcessorLogs("Backend: Test processing completed")
                .build();

        when(getMockedBackendClient().testProcessData(any(ModuleProcessRequest.class)))
                .thenReturn(Uni.createFrom().item(backendResponse));

        // Process and verify
        UniAssertSubscriber<ModuleProcessResponse> subscriber = getProxyProcessor()
                .testProcessData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ModuleProcessResponse response = subscriber.awaitItem().getItem();

        assertThat(response, notNullValue());
        assertThat(response.getSuccess(), is(true));
        assertThat(response.hasOutputDoc(), is(true));
        assertThat(response.getOutputDoc().getDocId(), is(equalTo(document.getDocId())));
        assertThat(response.getProcessorLogsList(), hasItem(containsString("Backend: Test processing completed")));
    }

    @Test
    void testGetServiceRegistration() {
        // Create request
        RegistrationRequest request = RegistrationRequest.newBuilder().build();

        // Mock the backend response
        ServiceRegistrationMetadata backendResponse = ServiceRegistrationMetadata.newBuilder()
                .setModuleName("test-module")
                .setVersion("1.0.0")
                .setHealthCheckPassed(true)
                .setHealthCheckMessage("Module is healthy")
                .setJsonConfigSchema("{\"type\":\"object\",\"properties\":{\"test\":true}}")
                // Include metadata expected by assertions
                .putMetadata("proxy_enabled", "true")
                .putMetadata("proxy_version", "test")
                .build();

        when(getMockedBackendClient().getServiceRegistration(any(RegistrationRequest.class)))
                .thenReturn(Uni.createFrom().item(backendResponse));

        // Process and verify
        UniAssertSubscriber<ServiceRegistrationMetadata> subscriber = getProxyProcessor()
                .getServiceRegistration(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ServiceRegistrationMetadata response = subscriber.awaitItem().getItem();

        assertThat(response, notNullValue());
        // The proxy normalizes/overrides moduleName to identify itself
        assertThat(response.getModuleName(), is(equalTo("proxy-module")));
        assertThat(response.getVersion(), is(equalTo("1.0.0")));
        assertThat(response.getHealthCheckPassed(), is(true));
        assertThat(response.getMetadataMap(), hasKey("proxy_enabled"));
        assertThat(response.getMetadataMap(), hasEntry("proxy_enabled", "true"));
        assertThat(response.getMetadataMap(), hasKey("proxy_version"));
    }

    @Test
    void testProcessDataError() {
        // Create test document
        PipeDoc document = PipeDoc.newBuilder()
                .setDocId(UUID.randomUUID().toString())
                .build();

        // Create request
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(document)
                .build();

        // Mock the backend to throw an exception
        when(getMockedBackendClient().processData(any(ModuleProcessRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Simulated backend error")));

        // Process and verify
        UniAssertSubscriber<ModuleProcessResponse> subscriber = getProxyProcessor()
                .processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ModuleProcessResponse response = subscriber.awaitItem().getItem();

        assertThat(response, notNullValue());
        assertThat(response.getSuccess(), is(false));
        assertThat(response.getProcessorLogsList(), hasItem(containsString("Proxy error: Simulated backend error")));
    }

    @Test
    void testBackendFailureResponse() {
        // Create test document
        PipeDoc document = PipeDoc.newBuilder()
                .setDocId(UUID.randomUUID().toString())
                .build();

        // Create request
        ModuleProcessRequest request = ModuleProcessRequest.newBuilder()
                .setDocument(document)
                .build();

        // Mock the backend to return a failure response
        ModuleProcessResponse backendResponse = ModuleProcessResponse.newBuilder()
                .setSuccess(false)
                .addProcessorLogs("Backend: Processing failed")
                .build();

        when(getMockedBackendClient().processData(any(ModuleProcessRequest.class)))
                .thenReturn(Uni.createFrom().item(backendResponse));

        // Process and verify
        UniAssertSubscriber<ModuleProcessResponse> subscriber = getProxyProcessor()
                .processData(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ModuleProcessResponse response = subscriber.awaitItem().getItem();

        assertThat(response, notNullValue());
        assertThat(response.getSuccess(), is(false));
        assertThat(response.getProcessorLogsList(), hasItem(containsString("Backend: Processing failed")));
    }

    @Test
    void testGetServiceRegistrationError() {
        // Create request
        RegistrationRequest request = RegistrationRequest.newBuilder().build();

        // Mock the backend to throw an exception
        when(getMockedBackendClient().getServiceRegistration(any(RegistrationRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Simulated registration error")));

        // Process and verify
        UniAssertSubscriber<ServiceRegistrationMetadata> subscriber = getProxyProcessor()
                .getServiceRegistration(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        ServiceRegistrationMetadata response = subscriber.awaitItem().getItem();

        assertThat(response, notNullValue());
        // The proxy sets moduleName to 'proxy-module' on failure recovery and a default version
        assertThat(response.getModuleName(), is(equalTo("proxy-module")));
        assertThat(response.getVersion(), is(equalTo("1.0.0")));
    }
}
