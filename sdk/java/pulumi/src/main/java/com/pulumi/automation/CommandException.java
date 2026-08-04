// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * {@link CommandException} is thrown when a command fails.
 */
public class CommandException extends AutomationException {
    private static final long serialVersionUID = 1L;

    public CommandException(CommandResult result) {
        super(result.toString());
    }
}
