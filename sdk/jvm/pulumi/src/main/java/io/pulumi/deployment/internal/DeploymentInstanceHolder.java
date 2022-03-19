package io.pulumi.deployment.internal;

import com.google.common.annotations.VisibleForTesting;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.DeploymentInstance;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class DeploymentInstanceHolder {

    private static final AtomicReference<DeploymentInstance> instance = new AtomicReference<>();
    private static final CompletableFutureLock lock = new CompletableFutureLock();

    /**
     * @throws IllegalStateException if called before 'run' was called
     */
    public static DeploymentInstance getInstance() {
        if (instance.get() == null) {
            throw new IllegalStateException("Trying to acquire Deployment#instance before 'run' was called.");
        }
        return instance.get();
    }

    @InternalUse
    @VisibleForTesting
    public static Optional<DeploymentInstance> getInstanceOptional() { // FIXME remove public
        return Optional.ofNullable(instance.get());
    }

    public static <T> CompletableFuture<T> withInstance(DeploymentInstance instance,
                                                        Supplier<CompletableFuture<T>> action) {
        return lock.acquire().thenCompose(__ -> {
            var oldInstance = DeploymentInstanceHolder.instance.getAndSet(instance);
            DeploymentInstanceHolder.instance.set(instance);
            return action.get().whenComplete((___, ____) -> {
                if (!DeploymentInstanceHolder.instance.compareAndSet(instance, oldInstance)) {
                    throw new IllegalStateException("DeploymentInstanceHolder lock failed");
                }
            });
        }).whenComplete((__, ___) -> lock.release());
    }
}

class CompletableFutureLock {
    private boolean locked;
    private final Queue<CompletableFuture<Void>> promises;

    public CompletableFutureLock() {
        locked = false;
        promises = new LinkedList<>();
    }

    public synchronized CompletableFuture<Void> acquire() {
        if (!locked) {
            locked = true;
            return CompletableFuture.completedFuture(null);
        } else {
            var promise = new CompletableFuture<Void>();
            promises.add(promise);
            return promise;
        }
    }

    public synchronized void release() {
        var p = promises.poll();
        if (p != null) {
            p.completeAsync(() -> null);
        } else {
            locked = false;
        }
    }
}
