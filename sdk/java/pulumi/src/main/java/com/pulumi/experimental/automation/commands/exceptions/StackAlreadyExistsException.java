// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.commands.exceptions;

import com.pulumi.experimental.automation.commands.CommandResult;

public class StackAlreadyExistsException extends CommandException {
    public StackAlreadyExistsException(CommandResult result) {
        super(result);
    }
}
