// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

/**
 * {@link CancellationTokenRegistration} represents a callback registered with
 * a {@link CancellationToken}. Closing the registration unregisters the
 * callback.
 */
public final class CancellationTokenRegistration implements AutoCloseable {
    private final CancellationToken token;

    CancellationTokenRegistration(CancellationToken token) {
        this.token = token;
    }

    @Override
    public void close() {
        token.unregister(this);
    }
}
