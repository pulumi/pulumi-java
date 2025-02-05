// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public class StackAlreadyExistsException extends CommandException {
    public StackAlreadyExistsException(CommandResult result) {
        super(result);
    }
}
