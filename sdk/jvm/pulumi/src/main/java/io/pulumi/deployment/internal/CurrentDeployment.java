package io.pulumi.deployment.internal;

import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Facilitates passing current Deployment implicitly through the use of a ThreadLocal store.
 */
@InternalUse
public final class CurrentDeployment {

    private static final ThreadLocal<Optional<Deployment>> currentDeployment =
            ThreadLocal.withInitial(() -> Optional.empty());

    public static <T> T withCurrentDeployment(Deployment deployment, Supplier<T> block) {
        var old = currentDeployment.get();
        try {
            currentDeployment.set(Optional.of(deployment));
            return block.get();
        } finally {
            currentDeployment.set(old);
        }
    }

    public static Optional<Deployment> tryGetCurrentDeployment() {
        return currentDeployment.get();
    }

    public static Deployment getCurrentDeploymentOrThrow() {
        var deployment = tryGetCurrentDeployment();
        if (deployment.isEmpty()) {
            throw new IllegalStateException("Trying to acquire Deployment#instance before 'run' was called.");
        }
        return deployment.get();
    }
}
