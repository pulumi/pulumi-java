// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.commands.exceptions;

import com.pulumi.experimental.automation.commands.CommandResult;

public final class ConcurrentUpdateException extends CommandException {
    public ConcurrentUpdateException(CommandResult result) {
        super(result);
    }
 }
