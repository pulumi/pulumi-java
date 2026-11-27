// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * {@link CancellationToken} propagates notification that an operation should
 * be canceled. Tokens are created and canceled through a
 * {@link CancellationTokenSource}.
 */
public final class CancellationToken {
    private static final CancellationToken NONE = new CancellationToken(null);

    @Nullable
    private final CancellationTokenSource source;

    CancellationToken(@Nullable CancellationTokenSource source) {
        this.source = source;
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
        return source != null && source.isCancellationRequested();
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
        if (source == null) {
            return new CancellationTokenRegistration(null);
        }
        return source.register(callback);
    }
}
