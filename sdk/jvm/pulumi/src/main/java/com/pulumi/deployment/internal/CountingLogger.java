package com.pulumi.deployment.internal;

import com.pulumi.core.internal.annotations.InternalUse;

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
