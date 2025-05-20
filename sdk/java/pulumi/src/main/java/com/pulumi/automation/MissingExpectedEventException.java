// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * An exception thrown when an expected event is missing.
 */
public class MissingExpectedEventException extends AutomationException {
    private final String name;

    public MissingExpectedEventException(String name, String message) {
        super(message);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
