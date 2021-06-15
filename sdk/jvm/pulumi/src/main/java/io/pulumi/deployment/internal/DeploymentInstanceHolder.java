package io.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Internal;
import io.pulumi.deployment.DeploymentInstance;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DeploymentInstanceHolder {

    // TODO: maybe using a state machine for the uninitialized and initialized deployment would make sense
    //       not only it need the deployment instance, but also a stack - initialized after 'run' is called
    //       and config, ale probably more stuff... it's a god object...
    private static final AtomicReference<DeploymentInstance> instance = new AtomicReference<>();

    /**
     * @throws IllegalStateException if called before 'run' was called
     */
    public static DeploymentInstance getInstance() {
        if (instance.get() == null) {
            throw new IllegalStateException("Trying to acquire Deployment#instance before 'run' was called.");
        }
        return instance.get();
    }

    @Internal
    @VisibleForTesting
    public static Optional<DeploymentInstance> getInstanceOptional() { // FIXME remove public
        return Optional.ofNullable(instance.get());
    }

    /**
     * @throws IllegalStateException if called more than once (the instance already set)
     */
    @Internal
    static void setInstance(@Nullable DeploymentInstance newInstance) {
        if (instance.get() != null) {
            throw new IllegalStateException("Deployment#instance should only be set once at the beginning of a 'run' call.");
        }
        instance.set(newInstance);
    }

    @Internal
    @VisibleForTesting
    static void internalUnsafeDestroyInstance() {
        instance.set(null);
    }

}
