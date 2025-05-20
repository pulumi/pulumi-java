package com.pulumi.deployment.internal;

import com.google.common.util.concurrent.ListenableFuture;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import com.pulumi.resources.Resource;
import io.grpc.ManagedChannelBuilder;
import pulumirpc.Provider.CallResponse;
import pulumirpc.Provider.InvokeResponse;
import pulumirpc.Resource.ReadResourceRequest;
import pulumirpc.Resource.ReadResourceResponse;
import pulumirpc.Resource.RegisterPackageRequest;
import pulumirpc.Resource.RegisterPackageResponse;
import pulumirpc.Resource.RegisterResourceOutputsRequest;
import pulumirpc.Resource.RegisterResourceRequest;
import pulumirpc.Resource.RegisterResourceResponse;
import pulumirpc.Resource.ResourceCallRequest;
import pulumirpc.Resource.ResourceInvokeRequest;
import pulumirpc.Resource.SupportsFeatureRequest;
import pulumirpc.Resource.SupportsFeatureResponse;
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
        return toContextAwareCompletableFuture(this.monitor.supportsFeature(request));
    }

    @Override
    public CompletableFuture<InvokeResponse> invokeAsync(ResourceInvokeRequest request) {
        return toContextAwareCompletableFuture(this.monitor.invoke(request));
    }

    @Override
    public CompletableFuture<CallResponse> callAsync(ResourceCallRequest request) {
        return toContextAwareCompletableFuture(this.monitor.call(request));
    }

    @Override
    public CompletableFuture<ReadResourceResponse> readResourceAsync(Resource unused, ReadResourceRequest request) {
        return toContextAwareCompletableFuture(this.monitor.readResource(request));
    }

    @Override
    public CompletableFuture<RegisterResourceResponse> registerResourceAsync(Resource unused, RegisterResourceRequest request) {
        return toContextAwareCompletableFuture(this.monitor.registerResource(request));
    }

    @Override
    public CompletableFuture<Void> registerResourceOutputsAsync(RegisterResourceOutputsRequest request) {
        return toContextAwareCompletableFuture(this.monitor.registerResourceOutputs(request)).thenApply(empty -> null);
    }

    @Override
    public CompletableFuture<RegisterPackageResponse> registerPackageAsync(RegisterPackageRequest request) {
        return toContextAwareCompletableFuture(this.monitor.registerPackage(request));
    }

    private <T> CompletableFuture<T> toContextAwareCompletableFuture(ListenableFuture<T> listenableFuture) {
        return ContextAwareCompletableFuture.wrap(toCompletableFuture(listenableFuture));
    }
}