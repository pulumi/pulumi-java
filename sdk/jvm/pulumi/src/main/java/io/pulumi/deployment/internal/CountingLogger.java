package io.pulumi.deployment.internal;

/**
 * A logger that counts the errors
 */
public interface CountingLogger {
    int getErrorCount();
    boolean hasLoggedErrors();
}
