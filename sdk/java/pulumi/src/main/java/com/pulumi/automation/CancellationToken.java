// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link CancellationToken} propagates notification that an operation should
 * be canceled. Tokens are created and canceled through a
 * {@link CancellationTokenSource}.
 */
public final class CancellationToken {
    private static final CancellationToken NONE = new CancellationToken(false);

    private final Object lock = new Object();
    private final Map<CancellationTokenRegistration, Runnable> registrations = new LinkedHashMap<>();
    private boolean cancellationRequested;

    CancellationToken(boolean canceled) {
        this.cancellationRequested = canceled;
    }

    /**
     * Returns a token that can never be canceled.
     *
     * @return the empty token
     */
    public static CancellationToken none() {
        return NONE;
    }

    /**
     * Gets whether cancellation has been requested for this token.
     *
     * @return true if cancellation has been requested
     */
    public boolean isCancellationRequested() {
        synchronized (lock) {
            return cancellationRequested;
        }
    }

    /**
     * Registers a callback that will be invoked when this token is canceled.
     * If the token is already canceled, the callback is invoked immediately on
     * the calling thread.
     *
     * @param callback the callback to invoke on cancellation
     * @return a registration that can be closed to unregister the callback
     */
    public CancellationTokenRegistration register(Runnable callback) {
        Objects.requireNonNull(callback);
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

    void requestCancellation() {
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

    void unregister(CancellationTokenRegistration registration) {
        synchronized (lock) {
            registrations.remove(registration);
        }
    }

    void clearRegistrations() {
        synchronized (lock) {
            registrations.clear();
        }
    }
}
