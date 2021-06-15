package io.pulumi.deployment;

import io.pulumi.deployment.internal.Engine;
import pulumirpc.EngineOuterClass;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MockEngine implements Engine {

    @Nullable
    private String rootResourceUrn;
    private final Object rootResourceUrnLock = new Object();

    public final List<String> errors = Collections.synchronizedList(new LinkedList<>());

    @Override
    public CompletableFuture<Void> logAsync(EngineOuterClass.LogRequest request) {
        if (request.getSeverity() == EngineOuterClass.LogSeverity.ERROR) {
            this.errors.add(request.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<EngineOuterClass.SetRootResourceResponse> setRootResourceAsync(EngineOuterClass.SetRootResourceRequest request) {
        synchronized (rootResourceUrnLock) {
            if (rootResourceUrn != null && !Objects.equals(rootResourceUrn, request.getUrn())) {
                throw new IllegalStateException(String.format(
                        "An invalid attempt to set the root resource to '%s' while it's already set to '%s'",
                        request.getUrn(), rootResourceUrn
                ));
            }
            rootResourceUrn = request.getUrn();
        }

        return CompletableFuture.completedFuture(EngineOuterClass.SetRootResourceResponse.newBuilder().build());
    }

    @Override
    public CompletableFuture<EngineOuterClass.GetRootResourceResponse> getRootResourceAsync(EngineOuterClass.GetRootResourceRequest request) {
        synchronized (rootResourceUrnLock) {
            if (rootResourceUrn == null)
                throw new IllegalStateException("Root resource is not set");

            return CompletableFuture.completedFuture(
                    EngineOuterClass.GetRootResourceResponse.newBuilder().setUrn(rootResourceUrn).build()
            );
        }
    }
}
