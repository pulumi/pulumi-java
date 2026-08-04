// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

/**
 * {@link ConcurrentUpdateException} is thrown when a stack is updated concurrently.
 */
public final class ConcurrentUpdateException extends CommandException {
    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateException(CommandResult result) {
        super(result);
    }
 }
