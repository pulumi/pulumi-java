// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * An exception thrown when a stack already exists.
 */
public class StackAlreadyExistsException extends CommandException {
    private static final long serialVersionUID = 1L;

    public StackAlreadyExistsException(CommandResult result) {
        super(result);
    }
}
