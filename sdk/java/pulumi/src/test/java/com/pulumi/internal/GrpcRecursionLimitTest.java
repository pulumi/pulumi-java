package com.pulumi.internal;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.deployment.internal.GrpcMonitor;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import pulumirpc.Resource.RegisterResourceRequest;
import pulumirpc.Resource.RegisterResourceResponse;
import pulumirpc.ResourceMonitorGrpc;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcRecursionLimitTest {

    // Protobuf stops at 100 levels of nesting by default, and one level of a Struct costs three of
    // them (Struct, map entry, Value), so this is comfortably past the limit a default marshaller
    // would enforce. See https://github.com/pulumi/pulumi-java/issues/2277.
    private static final int NESTING_DEPTH = 64;

    private static final String SERVICE_NAME = "pulumirpc.ResourceMonitor";

    @Test
    void monitorParsesDeeplyNestedResponses() throws Exception {
        var object = nestedStruct(NESTING_DEPTH);
        var server = ServerBuilder.forPort(0)
                .addService(new ResourceMonitorGrpc.ResourceMonitorImplBase() {
                    @Override
                    public void registerResource(
                            RegisterResourceRequest request,
                            StreamObserver<RegisterResourceResponse> responseObserver) {
                        responseObserver.onNext(RegisterResourceResponse.newBuilder()
                                .setUrn("urn:pulumi:test::test::test:index:Test::test")
                                .setObject(object)
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();
        try {
            var monitor = new GrpcMonitor("127.0.0.1:" + server.getPort());
            var response = monitor
                    .registerResourceAsync(null, RegisterResourceRequest.getDefaultInstance())
                    .get(30, TimeUnit.SECONDS);
            assertThat(response.getObject()).isEqualTo(object);
        } finally {
            server.shutdownNow();
            server.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void reboundServiceParsesDeeplyNestedRequests() {
        var request = RegisterResourceRequest.newBuilder()
                .setObject(nestedStruct(NESTING_DEPTH))
                .build();

        var service = GrpcRecursionLimit.intercept(
                new ResourceMonitorGrpc.ResourceMonitorImplBase() {}.bindService());
        var method = service.getMethod(SERVICE_NAME + "/RegisterResource");

        var parsed = method.getMethodDescriptor().getRequestMarshaller()
                .parse(new ByteArrayInputStream(request.toByteArray()));

        assertThat(parsed).isEqualTo(request);
    }

    @Test
    void reboundServiceKeepsItsMethodsAndHandlers() {
        var original = new ResourceMonitorGrpc.ResourceMonitorImplBase() {}.bindService();
        var rebound = GrpcRecursionLimit.intercept(original);

        assertThat(rebound.getServiceDescriptor().getName()).isEqualTo(SERVICE_NAME);
        assertThat(rebound.getMethods()).hasSameSizeAs(original.getMethods());
        for (var method : original.getMethods()) {
            var name = method.getMethodDescriptor().getFullMethodName();
            assertThat(rebound.getMethod(name)).isNotNull();
            assertThat(rebound.getMethod(name).getServerCallHandler())
                    .isSameAs(method.getServerCallHandler());
        }
    }

    private static Struct nestedStruct(int depth) {
        var struct = Struct.newBuilder()
                .putFields("leaf", Value.newBuilder().setStringValue("bottom").build())
                .build();
        for (var i = 0; i < depth; i++) {
            struct = Struct.newBuilder()
                    .putFields("nested", Value.newBuilder().setStructValue(struct).build())
                    .build();
        }
        return struct;
    }
}
