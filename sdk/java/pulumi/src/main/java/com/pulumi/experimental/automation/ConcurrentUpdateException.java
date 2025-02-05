// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

public final class ConcurrentUpdateException extends CommandException {
    public ConcurrentUpdateException(CommandResult result) {
        super(result);
    }
 }
