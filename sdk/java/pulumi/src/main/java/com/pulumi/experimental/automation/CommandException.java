// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public class CommandException extends AutomationException {
    public CommandException(CommandResult result) {
        super(result.toString());
    }
}
