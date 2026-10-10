// Copyright 2026, Pulumi Corporation

package com.pulumi.automation;

/**
 * An exception thrown when a command was canceled via a
 * {@link CancellationToken} before it could run to completion.
 */
public class CommandCanceledException extends CommandException {
    public CommandCanceledException(CommandResult result) {
        super(result);
    }
}
