package com.pulumi.test.internal;

import com.pulumi.deployment.internal.CountingLogger;
import com.pulumi.deployment.internal.Engine;
import pulumirpc.EngineOuterClass;

import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class MockEngine implements Engine, CountingLogger {

    private final AtomicReference<String> rootResourceUrn = new AtomicReference<>(null);
    private final Queue<String> errors = new ConcurrentLinkedQueue<>();

    @Override
    public int getErrorCount() {
        return errors.size();
    }

    @Override
    public boolean hasLoggedErrors() {
        return !errors.isEmpty();
    }

    @Override
    public CompletableFuture<Void> logAsync(EngineOuterClass.LogRequest request) {
        if (request.getSeverity() == EngineOuterClass.LogSeverity.ERROR) {
            this.errors.add(request.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    public Collection<String> getErrors() {
        return errors;
    }

    @Override
    public CompletableFuture<EngineOuterClass.GetRootResourceResponse> getRootResourceAsync(EngineOuterClass.GetRootResourceRequest request) {
        if (rootResourceUrn.get() == null) {
            throw new IllegalStateException("Root resource is not set");
        }

        return CompletableFuture.completedFuture(
                EngineOuterClass.GetRootResourceResponse.newBuilder()
                        .setUrn(rootResourceUrn.get())
                        .build()
        );
    }

    @Override
    public CompletableFuture<EngineOuterClass.RequirePulumiVersionResponse> requirePulumiVersionAsync(EngineOuterClass.RequirePulumiVersionRequest request) {
        // Noop
        return CompletableFuture.completedFuture(EngineOuterClass.RequirePulumiVersionResponse.newBuilder().build());
    }
}
