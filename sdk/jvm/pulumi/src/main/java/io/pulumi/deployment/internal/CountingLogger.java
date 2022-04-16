package io.pulumi.deployment.internal;

import io.pulumi.core.internal.annotations.InternalUse;

/**
 * A logger that counts the errors
 */
@InternalUse
public interface CountingLogger {
    @InternalUse
    int getErrorCount();
    @InternalUse
    boolean hasLoggedErrors();
}
