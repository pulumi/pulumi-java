// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

/**
 * {@link AutomationException} is a base class for all exceptions thrown by the
 * Automation API.
 */
public class AutomationException extends Exception {
    public AutomationException(String message) {
        super(message);
    }

    public AutomationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutomationException(Throwable cause) {
        super(cause);
    }
}
