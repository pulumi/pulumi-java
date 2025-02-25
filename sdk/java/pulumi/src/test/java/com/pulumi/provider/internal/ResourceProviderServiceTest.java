package com.pulumi.provider.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pulumirpc.ResourceProviderGrpc;
import com.pulumi.test.internal.PulumiTestInternal;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

class ResourceProviderServiceTest {
    private ResourceProviderService service;
    private ManagedChannel channel;
    private Provider mockProvider;
    private static final String SERVER_NAME = "test-server";

    class TestResourceProviderService extends ResourceProviderService {
        TestResourceProviderService(String engineAddress, Provider implementation) {
            super(engineAddress, implementation);
        }

        @Override
        protected ServerBuilder<?> createServerBuilder() {
            return InProcessServerBuilder.forName(SERVER_NAME);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        mockProvider = mock(Provider.class);
        
        service = new TestResourceProviderService(
            "dummy-engine-address",
            mockProvider
        );
        service.start();

        channel = InProcessChannelBuilder.forName(SERVER_NAME)
            .directExecutor()
            .build();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdown();
        service.server.shutdown();
        channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        service.server.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        PulumiTestInternal.cleanup();
    }

    @Test
    void testGetPluginInfo() {
        var stub = ResourceProviderGrpc.newBlockingStub(channel);
        var response = stub.getPluginInfo(Empty.getDefaultInstance());
        assertEquals("1.0.0", response.getVersion());
    }

    @Test
    void testGetSchemaHandlesImplementationError() {
        var expectedError = new RuntimeException("Test implementation error");
        when(mockProvider.getSchema(any()))
            .thenReturn(CompletableFuture.failedFuture(expectedError));

        var stub = ResourceProviderGrpc.newBlockingStub(channel);
        var request = pulumirpc.Provider.GetSchemaRequest.newBuilder().build();

        var exception = assertThrows(StatusRuntimeException.class, () -> 
            stub.getSchema(request)
        );
        
        assertEquals(io.grpc.Status.Code.INTERNAL, exception.getStatus().getCode());
        assertTrue(exception.getMessage().contains("Test implementation error"));
    }

    @Test
    void testConfigureHandlesImplementationError() {
        var expectedError = new IllegalArgumentException("Invalid configuration");
        when(mockProvider.configure(any()))
            .thenReturn(CompletableFuture.failedFuture(expectedError));

        var stub = ResourceProviderGrpc.newBlockingStub(channel);
        var request = pulumirpc.Provider.ConfigureRequest.newBuilder()
            .build();

        var exception = assertThrows(StatusRuntimeException.class, () -> 
            stub.configure(request)
        );
        
        assertEquals(io.grpc.Status.Code.INTERNAL, exception.getStatus().getCode());
        assertTrue(exception.getMessage().contains("Invalid configuration"));
    }

    @Test
    void testConstructHandlesImplementationError() {
        var expectedError = new RuntimeException("Construction failed");
        when(mockProvider.construct(any()))
            .thenReturn(CompletableFuture.failedFuture(expectedError));

        var stub = ResourceProviderGrpc.newBlockingStub(channel);
        var request = pulumirpc.Provider.ConstructRequest.newBuilder()
            .setType("test:index:Resource")
            .setName("test")
            .setParent("parent-urn")
            .setProject("test-project")
            .setStack("test-stack")
            .setMonitorEndpoint("test-monitor")
            .build();

        var exception = assertThrows(StatusRuntimeException.class, () -> 
            stub.construct(request)
        );
        
        assertEquals(io.grpc.Status.Code.INTERNAL, exception.getStatus().getCode());
        String message = exception.getMessage();
        if (!message.contains("Construction failed")) {
            assertEquals("Construction failed", message);
        }
    }

    @Test
    void testConstructHandlesMissingParent() {
        var stub = ResourceProviderGrpc.newBlockingStub(channel);
        var request = pulumirpc.Provider.ConstructRequest.newBuilder()
            .setType("test:index:Resource")
            .setName("test")
            // Deliberately omit parent URN
            .build();

        var exception = assertThrows(StatusRuntimeException.class, () -> 
            stub.construct(request)
        );
        
        assertEquals(io.grpc.Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
        assertTrue(exception.getMessage().contains("Parent must be set"));
    }
} 