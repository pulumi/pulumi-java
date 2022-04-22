package com.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.DeploymentInstance;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@InternalUse
public abstract class DeploymentInstanceHolder {

    // TODO: maybe using a state machine for the uninitialized and initialized deployment would make sense
    //       not only it need the deployment instance, but also a stack - initialized after 'run' is called
    //       and config, ale probably more stuff... it's a god object...
    private static final AtomicReference<DeploymentInstance> instance = new AtomicReference<>();

    /**
     * @throws IllegalStateException if called before 'run' was called
     */
    public static DeploymentInstance getInstance() {
        var i = instance.get();
        if (i == null) {
            throw new IllegalStateException("Trying to acquire Deployment#instance before 'run' was called.");
        }
        return i;
    }

    @InternalUse
    @VisibleForTesting
    public static Optional<DeploymentInstance> getInstanceOptional() { // FIXME remove public
        return Optional.ofNullable(instance.get());
    }

    /**
     * @throws IllegalStateException if called more than once (the instance already set)
     */
    @InternalUse
    static void setInstance(@Nullable DeploymentInstance newInstance) {
        if (!instance.compareAndSet(null, newInstance)) {
            throw new IllegalStateException("Deployment#instance should only be set once at the beginning of a 'run' call.");
        }
    }

    @InternalUse
    @VisibleForTesting
    static void internalUnsafeDestroyInstance() {
        instance.set(null);
    }

}
