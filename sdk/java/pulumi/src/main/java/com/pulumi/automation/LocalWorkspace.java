package com.pulumi.automation;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * LocalWorkspace is a default implementation of the {@link Workspace} interface.
 * <p>
 * LocalWorkspace relies on {@code Pulumi.yaml} and {@code Pulumi.<stack>.yaml}
 * as the intermediate format for Project and Stack settings.
 * Modifying ProjectSettings will alter the Workspace {@code Pulumi.yaml} file,
 * and setting config on a Stack will modify the {@code Pulumi.<stack>.yaml} file.
 * This is identical to the behavior of Pulumi CLI driven workspaces.
 * <p>
 * If not provided a working directory, causing LocalWorkspace to create a temp directory,
 * the temp directory will be cleaned up.
 */
public class LocalWorkspace implements Workspace {

    private final Logger logger;
    private final ProjectSettings settings;
    private final ImmutableMap<String, String> environmentVariables;
    private final LocalWorkspaceOptions options;

    public LocalWorkspace(
            Logger logger,
            ProjectSettings settings,
            Map<String, String> environmentVariables,
            LocalWorkspaceOptions options
    ) {
        this.logger = requireNonNull(logger);
        this.settings = requireNonNull(settings);
        this.environmentVariables = ImmutableMap.copyOf(environmentVariables);
        this.options = requireNonNull(options);
    }

    @Override
    public ProjectSettings projectSettings() {
        return this.settings;
    }

    @Override
    public ImmutableMap<String, String> environmentVariables() {
        return this.environmentVariables;
    }

    @Override
    public Path workDir() {
        return this.options.workDir();
    }

    @Override
    public Optional<Consumer<Context>> program() {
        return this.options.program();
    }

    @Override
    public WorkspaceStack upsertStack(StackSettings settings) {
        return new WorkspaceStack(logger, this, settings);
    }
}
