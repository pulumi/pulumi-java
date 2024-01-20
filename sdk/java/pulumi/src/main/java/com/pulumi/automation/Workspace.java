package com.pulumi.automation;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Workspace is the execution context containing a single Pulumi project,
 * a program, and multiple stacks.
 * <p>
 * Workspaces are used to manage the execution environment, providing various utilities
 * such as plugin installation, environment configuration ($PULUMI_HOME),
 * and creation, deletion, and listing of Stacks.
 */
public interface Workspace {
    ProjectSettings projectSettings();
    ImmutableMap<String, String> environmentVariables();
    Path workDir();
    Optional<Consumer<Context>> program();
    WorkspaceStack upsertStack(StackSettings options);
}
