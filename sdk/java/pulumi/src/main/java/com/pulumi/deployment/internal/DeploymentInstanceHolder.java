package com.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.DeploymentInstance;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The Java provider assumes that there's an ambient authority to track the deployment.
 * This is implemented as value in the Thread Local Storage (TLS).
 * <p>
 * That is fine for simple sequential code, but the Pulumi system is highly asynchronous.
 * The Java runtime for asynchronous computations relies on thread pool controlled by the system, not the application.
 * <p>
 * This creates a problem, because the computation of the Pulumi App can resume on a fresh thread with no context.
 * To address this issue, we have {@link com.pulumi.core.internal.ContextAwareCompletableFuture}
 * <p>
 * That class implements the same interface as {@link java.util.concurrent.CompletableFuture}, with the addition of preserving the context.
 * When a future is completed or chained, it injects the context into the TLS on all resume points.
 */
@InternalUse
public abstract class DeploymentInstanceHolder {
    private static final ThreadLocal<DeploymentInstance> instance = new ThreadLocal<>();

    /**
     * @throws IllegalStateException if called before 'run' was called
     */
    public static DeploymentInstance getInstance() {
        var value = instance.get();
        if (value == null) {
            throw new IllegalStateException("Trying to acquire Deployment#instance before 'run' was called.");
        }
        if (value.isInvalid()) {
            throw new IllegalStateException("Trying to acquire Deployment#instance after 'run' was called.");
        }

        return value;
    }

    @InternalUse
    @VisibleForTesting
    public static DeploymentInstance getInstanceNoThrow() {
        var value = instance.get();
        if (value != null && value.isInvalid()) {
            value = null;
        }

        return value;
    }

    @InternalUse
    @VisibleForTesting
    public static Optional<DeploymentInstance> getInstanceOptional() { // FIXME remove public
        return Optional.ofNullable(getInstanceNoThrow());
    }

    @InternalUse
    public static void setInstance(@Nullable DeploymentInstance newInstance) {
        // Because of thread reentrancy, we can no longer enforce single assignment.
        instance.set(newInstance);
    }

    @InternalUse
    @VisibleForTesting
    public static void internalUnsafeDestroyInstance() {
        var value = getInstanceNoThrow();
        if (value != null) {
            value.markInvalid();
            setInstance(null);
        }
    }
}
