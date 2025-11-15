package ai.pipestream.module.proxy;

import ai.pipestream.data.module.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * Proxy implementation of the PipeStepProcessor interface.
 * Forwards all calls to the configured backend module.
 */
@GrpcService
@Singleton
public class PipeStepProcessorProxy implements PipeStepProcessor {
    private static final Logger LOG = Logger.getLogger(PipeStepProcessorProxy.class);
    
    private final MutinyPipeStepProcessorGrpc.MutinyPipeStepProcessorStub moduleClient;
    private final MeterRegistry registry;
    
    private Counter processedRequests;
    private Counter failedRequests;
    private Timer processingTimer;
    
    @Inject
    public PipeStepProcessorProxy(ModuleClientFactory clientFactory, MeterRegistry registry) {
        this.moduleClient = clientFactory.createClient();
        this.registry = registry;
        initMetrics();
    }
    
    private void initMetrics() {
        this.processedRequests = Counter.builder("proxy.requests.processed")
                .description("Number of requests processed by the proxy")
                .register(registry);
                
        this.failedRequests = Counter.builder("proxy.requests.failed")
                .description("Number of requests that failed processing")
                .register(registry);
                
        this.processingTimer = Timer.builder("proxy.processing.time")
                .description("Time taken to process requests through the proxy")
                .register(registry);
    }
    
    @Override
    public Uni<ModuleProcessResponse> processData(ModuleProcessRequest request) {
        LOG.debugf("Forwarding processData request for document: %s",
                request.hasDocument() ? request.getDocument().getDocId() : "no-document");
        
        return processingTimer.record(() -> 
            moduleClient.processData(request)
                .onItem().invoke(response -> {
                    if (response.getSuccess()) {
                        processedRequests.increment();
                        LOG.debugf("Successfully processed document: %s",
                            request.hasDocument() ? request.getDocument().getDocId() : "no-document");
                    } else {
                        failedRequests.increment();
                        LOG.warnf("Failed to process document: %s",
                            request.hasDocument() ? request.getDocument().getDocId() : "no-document");
                    }
                })
                .onFailure().invoke(error -> {
                    failedRequests.increment();
                    LOG.errorf(error, "Error processing document: %s",
                        request.hasDocument() ? request.getDocument().getDocId() : "no-document");
                })
                .onFailure().recoverWithItem(error -> {
                    // Create a failure response if the backend call fails
                    return ModuleProcessResponse.newBuilder()
                            .setSuccess(false)
                            .addProcessorLogs("Proxy error: " + error.getMessage())
                            .build();
                })
        );
    }
    
    @Override
    public Uni<ModuleProcessResponse> testProcessData(ModuleProcessRequest request) {
        LOG.debugf("Forwarding testProcessData request");
        
        return moduleClient.testProcessData(request)
                .onItem().invoke(response -> LOG.debugf("Test processing completed with success: %s", response.getSuccess()))
                .onFailure().invoke(error -> LOG.errorf(error, "Error in test processing"))
                .onFailure().recoverWithItem(error -> {
                    // Create a failure response if the backend call fails
                    return ModuleProcessResponse.newBuilder()
                            .setSuccess(false)
                            .addProcessorLogs("Proxy error in test processing: " + error.getMessage())
                            .build();
                });
    }
    
    @Override
    public Uni<ServiceRegistrationMetadata> getServiceRegistration(RegistrationRequest request) {
        LOG.debugf("Forwarding getServiceRegistration request");
        
        return moduleClient.getServiceRegistration(request)
                .onItem().transform(response -> {
                    // Enhance the registration response with proxy info
                    return ServiceRegistrationMetadata.newBuilder(response)
                            .setModuleName("proxy-module")
                            .build();
                })
                .onFailure().invoke(error -> LOG.errorf(error, "Error getting service registration"))
                .onFailure().recoverWithItem(error -> {
                    // Create a minimal registration response if the backend call fails
                    return ServiceRegistrationMetadata.newBuilder()
                            .setModuleName("proxy-module")
                            .setVersion("1.0.0")
                            .build();
                });
    }
}