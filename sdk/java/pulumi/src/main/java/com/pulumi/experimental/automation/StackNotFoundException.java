// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public class StackNotFoundException extends CommandException {
    public StackNotFoundException(CommandResult result) {
        super(result);
    }
}
