// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * An exception thrown when a stack is not found.
 */
public class StackNotFoundException extends CommandException {
    private static final long serialVersionUID = 1L;

    public StackNotFoundException(CommandResult result) {
        super(result);
    }
}
