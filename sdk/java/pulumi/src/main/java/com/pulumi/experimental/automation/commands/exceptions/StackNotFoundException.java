// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.commands.exceptions;

import com.pulumi.experimental.automation.commands.CommandResult;

public class StackNotFoundException extends CommandException {
    public StackNotFoundException(CommandResult result) {
        super(result);
    }
}
