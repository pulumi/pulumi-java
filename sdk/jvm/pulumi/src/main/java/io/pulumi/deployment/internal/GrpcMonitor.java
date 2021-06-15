package io.pulumi.deployment.internal;

import io.grpc.ManagedChannelBuilder;
import io.pulumi.resources.Resource;
import pulumirpc.Provider.InvokeRequest;
import pulumirpc.Provider.InvokeResponse;
import pulumirpc.Resource.*;
import pulumirpc.ResourceMonitorGrpc;

import java.util.concurrent.CompletableFuture;

import static net.javacrumbs.futureconverter.java8guava.FutureConverter.toCompletableFuture;
import static pulumirpc.ResourceMonitorGrpc.newFutureStub;

public class GrpcMonitor implements Monitor {
    private final ResourceMonitorGrpc.ResourceMonitorFutureStub monitor;

    public GrpcMonitor(String monitor) {
        // maxRpcMessageSize raises the gRPC Max Message size from `4194304` (4mb) to `419430400` (400mb)
        var maxRpcMessageSizeInBytes = 400 * 1024 * 1024;
        this.monitor = newFutureStub(
                ManagedChannelBuilder
                        .forTarget(monitor)
                        .usePlaintext() // disable TLS
                        .maxInboundMessageSize(maxRpcMessageSizeInBytes)
                        .build()
        );
    }

    @Override
    public CompletableFuture<SupportsFeatureResponse> supportsFeatureAsync(SupportsFeatureRequest request) {
        return toCompletableFuture(this.monitor.supportsFeature(request));
    }

    @Override
    public CompletableFuture<InvokeResponse> invokeAsync(InvokeRequest request) {
        return toCompletableFuture(this.monitor.invoke(request));
    }

    @Override
    public CompletableFuture<ReadResourceResponse> readResourceAsync(Resource unused, ReadResourceRequest request) {
        return toCompletableFuture(this.monitor.readResource(request));
    }

    @Override
    public CompletableFuture<RegisterResourceResponse> registerResourceAsync(Resource unused, RegisterResourceRequest request) {
        return toCompletableFuture(this.monitor.registerResource(request));
    }

    @Override
    public CompletableFuture<Void> registerResourceOutputsAsync(RegisterResourceOutputsRequest request) {
        return toCompletableFuture(this.monitor.registerResourceOutputs(request)).thenApply(empty -> null);
    }
}
