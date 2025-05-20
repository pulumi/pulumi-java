package com.pulumi.deployment.internal;

import com.google.common.util.concurrent.ListenableFuture;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import io.grpc.ManagedChannelBuilder;
import pulumirpc.EngineGrpc;
import pulumirpc.EngineOuterClass.GetRootResourceRequest;
import pulumirpc.EngineOuterClass.GetRootResourceResponse;
import pulumirpc.EngineOuterClass.LogRequest;
import pulumirpc.EngineOuterClass.SetRootResourceRequest;
import pulumirpc.EngineOuterClass.SetRootResourceResponse;

import java.util.concurrent.CompletableFuture;

import static net.javacrumbs.futureconverter.java8guava.FutureConverter.toCompletableFuture;
import static pulumirpc.EngineGrpc.newFutureStub;

public class GrpcEngine implements Engine {

    private final EngineGrpc.EngineFutureStub engine;

    public GrpcEngine(String engine) {
        // maxRpcMessageSize raises the gRPC Max Message size from `4194304` (4mb) to `419430400` (400mb)
        var maxRpcMessageSizeInBytes = 400 * 1024 * 1024;
        this.engine = newFutureStub(
                ManagedChannelBuilder
                        .forTarget(engine)
                        .usePlaintext() // disable TLS
                        .maxInboundMessageSize(maxRpcMessageSizeInBytes)
                        .build()
        );
    }

    @Override
    public CompletableFuture<Void> logAsync(LogRequest request) {
        return toContextAwareCompletableFuture(this.engine.log(request)).thenApply(empty -> null);
    }

    @Override
    public CompletableFuture<SetRootResourceResponse> setRootResourceAsync(SetRootResourceRequest request) {
        return toContextAwareCompletableFuture(this.engine.setRootResource(request));
    }

    @Override
    public CompletableFuture<GetRootResourceResponse> getRootResourceAsync(GetRootResourceRequest request) {
        return toContextAwareCompletableFuture(this.engine.getRootResource(request));
    }

    private <T> CompletableFuture<T> toContextAwareCompletableFuture(ListenableFuture<T> listenableFuture) {
        return ContextAwareCompletableFuture.wrap(toCompletableFuture(listenableFuture));
    }
}
