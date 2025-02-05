// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A Pulumi command.
 */
public interface PulumiCommand {
    /**
     * The version of the Pulumi CLI that is being used.
     *
     * @return the version of the Pulumi CLI or {@code null}
     */
    @Nullable
    Version getVersion();

    /**
     * Runs the Pulumi command.
     *
     * @param args    the arguments to pass to the command
     * @param options the options for running the command
     * @return the command result
     * @throws AutomationException if the command fails
     */
    CommandResult run(
            List<String> args,
            CommandRunOptions options) throws AutomationException;

    /**
     * Runs the Pulumi command with default options.
     *
     * @param args the arguments to pass to the command
     * @return the command result
     * @throws AutomationException if the command fails
     */
    default CommandResult run(List<String> args) throws AutomationException {
        return run(args, CommandRunOptions.EMPTY);
    }
}
