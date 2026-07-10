// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

/**
 * {@link CancellationTokenSource} signals to a {@link CancellationToken} that
 * it should be canceled.
 */
public final class CancellationTokenSource implements AutoCloseable {
    private final CancellationToken token = new CancellationToken(false);

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
        return token.isCancellationRequested();
    }

    /**
     * Communicates a request for cancellation. Subsequent calls have no
     * effect.
     */
    public void cancel() {
        token.requestCancellation();
    }

    @Override
    public void close() {
        token.clearRegistrations();
    }
}
