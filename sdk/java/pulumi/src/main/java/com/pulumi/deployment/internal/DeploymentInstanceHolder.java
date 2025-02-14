package com.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.DeploymentInstance;

import javax.annotation.Nullable;
import java.util.Optional;

@InternalUse
public abstract class DeploymentInstanceHolder {

    private static final ThreadLocal<DeploymentInstance> instance = new ThreadLocal<>();

    /**
     * @throws IllegalStateException if called before 'run' was called
     */
    public static DeploymentInstance getInstance() {
        var i = getInstanceNoThrow();
        if (i == null) {
            throw new IllegalStateException("Trying to acquire Deployment#instance before 'run' was called.");
        }
        return i;
    }

    @InternalUse
    @VisibleForTesting
    public static DeploymentInstance getInstanceNoThrow() {
        return instance.get();
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
        setInstance(null);
    }
}
