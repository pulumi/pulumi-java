// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

/**
 * {@link CommandException} is thrown when a command fails.
 */
public class CommandException extends AutomationException {
    public CommandException(CommandResult result) {
        super(result.toString());
    }
}
