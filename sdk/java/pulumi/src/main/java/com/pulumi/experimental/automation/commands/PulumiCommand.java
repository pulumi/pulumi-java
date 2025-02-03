// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.commands;

import java.util.List;

import javax.annotation.Nullable;

import com.pulumi.experimental.automation.exceptions.AutomationException;

public abstract class PulumiCommand {
    /**
     * The version of the Pulumi CLI that is being used.
     *
     * @return the version of the Pulumi CLI or {@code null}
     */
    @Nullable
    public abstract PulumiVersion getVersion();

    /**
     * Runs the Pulumi command.
     *
     * @param args the arguments to pass to the command
     * @return the command result
     * @throws AutomationException if the command fails
     */
    public CommandResult run(List<String> args) throws AutomationException {
        return run(args, CommandRunOptions.EMPTY);
    }

    /**
     * Runs the Pulumi command.
     *
     * @param args    the arguments to pass to the command
     * @param options the options for running the command
     * @return the command result
     * @throws AutomationException if the command fails
     */
    public abstract CommandResult run(
            List<String> args,
            CommandRunOptions options) throws AutomationException;
}
