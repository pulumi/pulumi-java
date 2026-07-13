// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link CancellationTokenSource} signals to a {@link CancellationToken} that
 * it should be canceled.
 */
public final class CancellationTokenSource implements AutoCloseable {
    private final Object lock = new Object();
    private final Map<CancellationTokenRegistration, Runnable> registrations = new LinkedHashMap<>();
    private final CancellationToken token = new CancellationToken(this);
    private boolean cancellationRequested;

    /**
     * Gets the {@link CancellationToken} associated with this source.
     *
     * @return the token
     */
    public CancellationToken token() {
        return token;
    }

    /**
     * Gets whether cancellation has been requested for this source.
     *
     * @return true if cancellation has been requested
     */
    public boolean isCancellationRequested() {
        synchronized (lock) {
            return cancellationRequested;
        }
    }

    /**
     * Communicates a request for cancellation. Subsequent calls have no
     * effect.
     */
    public void cancel() {
        Map<CancellationTokenRegistration, Runnable> toInvoke;
        synchronized (lock) {
            if (cancellationRequested) {
                return;
            }
            cancellationRequested = true;
            toInvoke = new LinkedHashMap<>(registrations);
            registrations.clear();
        }
        for (var callback : toInvoke.values()) {
            callback.run();
        }
    }

    CancellationTokenRegistration register(Runnable callback) {
        var registration = new CancellationTokenRegistration(this);
        boolean invokeNow;
        synchronized (lock) {
            invokeNow = cancellationRequested;
            if (!invokeNow) {
                registrations.put(registration, callback);
            }
        }
        if (invokeNow) {
            callback.run();
        }
        return registration;
    }

    void unregister(CancellationTokenRegistration registration) {
        synchronized (lock) {
            registrations.remove(registration);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            registrations.clear();
        }
    }
}
