// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.commands.exceptions;

import com.pulumi.experimental.automation.commands.CommandResult;
import com.pulumi.experimental.automation.exceptions.AutomationException;

public class CommandException extends AutomationException {
    public CommandException(CommandResult result) {
        super(result.toString());
    }
}
