// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * An exception thrown when a stack is not found.
 */
public class StackNotFoundException extends CommandException {
    public StackNotFoundException(CommandResult result) {
        super(result);
    }
}
