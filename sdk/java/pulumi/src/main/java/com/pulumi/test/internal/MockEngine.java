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

/**
 * MockEngine is a test implementation of the {@link Engine} and {@link CountingLogger} interfaces for unit tests.
 *
 * @see com.pulumi.deployment.internal.Engine
 * @see com.pulumi.deployment.internal.CountingLogger
 */
public class MockEngine implements Engine, CountingLogger {

    /**
     * The URN of the root resource for the test deployment, managed atomically.
     */
    private final AtomicReference<String> rootResourceUrn = new AtomicReference<>(null);
    /**
     * A thread-safe queue of error messages logged during the test execution.
     */
    private final Queue<String> errors = new ConcurrentLinkedQueue<>();

    /**
     * Returns the number of error messages logged by this engine.
     *
     * @return the count of logged errors
     */
    @Override
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Checks if any error messages have been logged.
     *
     * @return true if errors have been logged, false otherwise
     */
    @Override
    public boolean hasLoggedErrors() {
        return !errors.isEmpty();
    }

    /**
     * Logs a message asynchronously. If the message severity is ERROR, it is tracked in the error queue.
     *
     * @param request the log request containing the message and severity
     * @return a completed future when logging is done
     */
    @Override
    public CompletableFuture<Void> logAsync(EngineOuterClass.LogRequest request) {
        if (request.getSeverity() == EngineOuterClass.LogSeverity.ERROR) {
            this.errors.add(request.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns a collection of all error messages logged during the test.
     *
     * @return a collection of error messages
     */
    public Collection<String> getErrors() {
        return errors;
    }

    /**
     * Sets the root resource URN for the test deployment. Throws if already set to a different value.
     *
     * @param request the set root resource request containing the URN
     * @return a completed future with the set root resource response
     * @throws IllegalStateException if the root resource is already set to a different URN
     */
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

    /**
     * Gets the root resource URN for the test deployment.
     *
     * @param request the get root resource request
     * @return a completed future with the get root resource response containing the URN
     * @throws IllegalStateException if the root resource has not been set
     */
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
