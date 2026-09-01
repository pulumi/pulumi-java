// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

import javax.annotation.Nullable;

/**
 * {@link CancellationTokenRegistration} represents a callback registered with
 * a {@link CancellationToken}. Closing the registration unregisters the
 * callback.
 */
public final class CancellationTokenRegistration implements AutoCloseable {
    @Nullable
    private final CancellationTokenSource source;

    CancellationTokenRegistration(@Nullable CancellationTokenSource source) {
        this.source = source;
    }

    @Override
    public void close() {
        if (source != null) {
            source.unregister(this);
        }
    }
}
