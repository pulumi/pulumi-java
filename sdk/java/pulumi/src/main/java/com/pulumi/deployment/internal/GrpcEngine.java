package com.pulumi.deployment.internal;

import com.google.common.util.concurrent.ListenableFuture;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import io.grpc.ManagedChannelBuilder;
import pulumirpc.EngineGrpc;
import pulumirpc.EngineOuterClass.GetRootResourceRequest;
import pulumirpc.EngineOuterClass.GetRootResourceResponse;
import pulumirpc.EngineOuterClass.LogRequest;
import pulumirpc.EngineOuterClass.RequirePulumiVersionRequest;
import pulumirpc.EngineOuterClass.RequirePulumiVersionResponse;

import java.util.concurrent.CompletableFuture;

import static net.javacrumbs.futureconverter.java8guava.FutureConverter.toCompletableFuture;
import static pulumirpc.EngineGrpc.newFutureStub;

public class GrpcEngine implements Engine {

    private final EngineGrpc.EngineFutureStub engine;

    public GrpcEngine(String engine) {
        // maxRpcMessageSize raises the gRPC Max Message size from `4194304` (4mb) to `419430400` (400mb)
        var maxRpcMessageSizeInBytes = 400 * 1024 * 1024;
        var channelBuilder = ManagedChannelBuilder
                .forTarget(engine)
                .usePlaintext() // disable TLS
                .maxInboundMessageSize(maxRpcMessageSizeInBytes);
        var interceptor = Instrumentation.getClientInterceptor();
        if (interceptor != null) {
            channelBuilder.intercept(interceptor);
        }
        this.engine = newFutureStub(channelBuilder.build());
    }

    @Override
    public CompletableFuture<Void> logAsync(LogRequest request) {
        return toContextAwareCompletableFuture(this.engine.log(request)).thenApply(empty -> null);
    }

    @Override
    public CompletableFuture<GetRootResourceResponse> getRootResourceAsync(GetRootResourceRequest request) {
        return toContextAwareCompletableFuture(this.engine.getRootResource(request));
    }

    @Override
    public CompletableFuture<RequirePulumiVersionResponse> requirePulumiVersionAsync(RequirePulumiVersionRequest request) {
        return toContextAwareCompletableFuture(this.engine.requirePulumiVersion(request));
    }

    private <T> CompletableFuture<T> toContextAwareCompletableFuture(ListenableFuture<T> listenableFuture) {
        return ContextAwareCompletableFuture.wrap(toCompletableFuture(listenableFuture));
    }
}
