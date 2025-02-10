// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * {@link ConcurrentUpdateException} is thrown when a stack is updated concurrently.
 */
public final class ConcurrentUpdateException extends CommandException {
    public ConcurrentUpdateException(CommandResult result) {
        super(result);
    }
 }
