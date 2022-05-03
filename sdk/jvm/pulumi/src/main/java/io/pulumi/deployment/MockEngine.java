package io.pulumi.deployment;

import io.pulumi.deployment.internal.CountingLogger;
import io.pulumi.deployment.internal.Engine;
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
    public CompletableFuture<EngineOuterClass.SetRootResourceResponse> setRootResourceAsync(EngineOuterClass.SetRootResourceRequest request) {
        if (rootResourceUrn.get() != null && !Objects.equals(rootResourceUrn.get(), request.getUrn())) {
            throw new IllegalStateException(String.format(
                    "An invalid attempt to set the root resource to '%s' while it's already set to '%s'",
                    request.getUrn(), rootResourceUrn.get()
            ));
        }
        rootResourceUrn.set(request.getUrn());


        return CompletableFuture.completedFuture(EngineOuterClass.SetRootResourceResponse.newBuilder().build());
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
}
