// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.pulumi.Context;

/**
 * Extensibility options to configure a {@link LocalWorkspace}; e.g: settings to
 * seed and environment variables to pass through to every command.
 */
public class LocalWorkspaceOptions {
    /**
     * An empty set of options.
     */
    public static final LocalWorkspaceOptions EMPTY = LocalWorkspaceOptions.builder().build();

    @Nullable
    private final Path workDir;
    @Nullable
    private final Path pulumiHome;
    @Nullable
    private final PulumiCommand pulumiCommand;
    @Nullable
    private final String secretsProvider;
    @Nullable
    private final Consumer<Context> program;
    @Nullable
    private final Logger logger;
    private final Map<String, String> environmentVariables;
    @Nullable
    private final ProjectSettings projectSettings;
    private final Map<String, StackSettings> stackSettings;

    private LocalWorkspaceOptions(Builder builder) {
        this.workDir = builder.workDir;
        this.pulumiHome = builder.pulumiHome;
        this.pulumiCommand = builder.pulumiCommand;
        this.secretsProvider = builder.secretsProvider;
        this.program = builder.program;
        this.logger = builder.logger;
        this.environmentVariables = builder.environmentVariables == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.environmentVariables);
        this.projectSettings = builder.projectSettings;
        this.stackSettings = builder.stackSettings == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(builder.stackSettings);
    }

    /**
     * Returns a new builder for {@link LocalWorkspaceOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new {@link LocalWorkspaceOptions} with the given program
     * if one is not already set.
     *
     * @param program         the program to set, if not already set
     * @param projectSettings the project settings to set, if not already set
     * @return the new {@link LocalWorkspaceOptions}
     */
    LocalWorkspaceOptions forInlineProgram(Consumer<Context> program, ProjectSettings projectSettings) {
        return toBuilder()
                .program(this.program != null ? this.program : program)
                .projectSettings(this.projectSettings != null ? this.projectSettings : projectSettings)
                .build();
    }

    /**
     * Returns a new {@link LocalWorkspaceOptions} with the given work directory
     * if one is not already set.
     *
     * @param workDir the work directory to set, if not already set
     * @return the new {@link LocalWorkspaceOptions}
     */
    LocalWorkspaceOptions forLocalProgram(Path workDir) {
        return toBuilder().workDir(this.workDir != null ? this.workDir : workDir).build();
    }

    /**
     * The directory to run Pulumi commands and read settings (Pulumi.yaml and
     * Pulumi.{stack}.yaml).
     *
     * @return the work directory
     */
    @Nullable
    public Path workDir() {
        return workDir;
    }

    /**
     * The directory to override for CLI metadata.
     *
     * @return the pulumi home directory
     */
    @Nullable
    public Path pulumiHome() {
        return pulumiHome;
    }

    /**
     * The Pulumi CLI installation to use.
     *
     * @return the pulumi command
     */
    @Nullable
    public PulumiCommand pulumiCommand() {
        return pulumiCommand;
    }

    /**
     * The secrets provider to use for encryption and decryption of stack secrets.
     * <p>
     * See:
     * https://www.pulumi.com/docs/intro/concepts/secrets/#available-encryption-providers
     *
     * @return the secrets provider
     */
    @Nullable
    public String secretsProvider() {
        return secretsProvider;
    }

    /**
     * The inline program to be used for Preview/Update operations if any.
     * <p>
     * If none is specified, the stack will refer to {@link ProjectSettings} for
     * this information.
     *
     * @return the program
     */
    @Nullable
    public Consumer<Context> program() {
        return program;
    }

    /**
     * A custom logger instance that will be used for inline programs. Note that it
     * will only be used if an inline program is also provided.
     *
     * @return the logger
     */
    @Nullable
    public Logger logger() {
        return logger;
    }

    /**
     * Environment values scoped to the current workspace. These will be supplied to
     * every Pulumi command.
     *
     * @return the environment variables
     */
    public Map<String, String> environmentVariables() {
        return environmentVariables;
    }

    /**
     * The settings object for the current project.
     * <p>
     * If provided when initializing {@link LocalWorkspace} a project settings
     * file will be written to when the workspace is initialized via
     * {@link LocalWorkspace#saveProjectSettings(ProjectSettings)}
     *
     * @return the project settings
     */
    @Nullable
    public ProjectSettings projectSettings() {
        return projectSettings;
    }

    /**
     * A map of Stack names and corresponding settings objects.
     * <p>
     * If provided when initializing {@link LocalWorkspace} stack settings
     * file(s) will be written to when the workspace is initialized via
     * {@link LocalWorkspace#saveStackSettings(String, StackSettings)}
     *
     * @return the stack settings
     */
    public Map<String, StackSettings> stackSettings() {
        return stackSettings;
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    private Builder toBuilder() {
        return new Builder()
                .workDir(workDir)
                .pulumiHome(pulumiHome)
                .pulumiCommand(pulumiCommand)
                .secretsProvider(secretsProvider)
                .program(program)
                .logger(logger)
                .environmentVariables(environmentVariables)
                .projectSettings(projectSettings)
                .stackSettings(stackSettings);
    }

    /**
     * Builder for {@link LocalWorkspaceOptions}.
     */
    public static class Builder {
        @Nullable
        private Path workDir;
        @Nullable
        private Path pulumiHome;
        @Nullable
        private PulumiCommand pulumiCommand;
        @Nullable
        private String secretsProvider;
        @Nullable
        private Consumer<Context> program;
        @Nullable
        private Logger logger;
        @Nullable
        private Map<String, String> environmentVariables;
        @Nullable
        private ProjectSettings projectSettings;
        @Nullable
        private Map<String, StackSettings> stackSettings;

        private Builder() {
        }

        /**
         * The directory to run Pulumi commands and read settings (Pulumi.yaml and
         * Pulumi.{stack}.yaml).
         *
         * @param workDir the work directory
         * @return the builder
         */
        public Builder workDir(Path workDir) {
            this.workDir = workDir;
            return this;
        }

        /**
         * The directory to override for CLI metadata.
         *
         * @param pulumiHome the pulumi home directory
         * @return the builder
         */
        public Builder pulumiHome(Path pulumiHome) {
            this.pulumiHome = pulumiHome;
            return this;
        }

        /**
         * The Pulumi CLI installation to use.
         *
         * @param pulumiCommand the pulumi command
         * @return the builder
         */
        public Builder pulumiCommand(PulumiCommand pulumiCommand) {
            this.pulumiCommand = pulumiCommand;
            return this;
        }

        /**
         * The secrets provider to use for encryption and decryption of stack secrets.
         * <p>
         * See:
         * https://www.pulumi.com/docs/intro/concepts/secrets/#available-encryption-providers
         *
         * @param secretsProvider the secrets provider
         * @return the builder
         */
        public Builder secretsProvider(String secretsProvider) {
            this.secretsProvider = secretsProvider;
            return this;
        }

        /**
         * The inline program to be used for Preview/Update operations.
         * <p>
         * If none is specified, the stack will refer to {@link ProjectSettings} for
         * this information.
         *
         * @param program the program
         * @return the builder
         */
        public Builder program(Consumer<Context> program) {
            this.program = program;
            return this;
        }

        /**
         * A custom logger instance that will be used for inline programs. Note that it
         * will only be used if an inline program is also provided.
         *
         * @param logger the logger
         * @return the builder
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Environment values scoped to the current workspace. These will be supplied to
         * every Pulumi command.
         *
         * @param environmentVariables the environment variables
         * @return the builder
         */
        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        /**
         * The settings object for the current project.
         * <p>
         * If provided when initializing {@link LocalWorkspace} a project settings
         * file will be written to when the workspace is initialized via
         * {@link LocalWorkspace#saveProjectSettings(ProjectSettings)}
         *
         * @param projectSettings the project settings
         * @return the builder
         */
        public Builder projectSettings(ProjectSettings projectSettings) {
            this.projectSettings = projectSettings;
            return this;
        }

        /**
         * A map of Stack names and corresponding settings objects.
         * <p>
         * If provided when initializing {@link LocalWorkspace} stack settings
         * file(s) will be written to when the workspace is initialized via
         * {@link LocalWorkspace#saveStackSettings(String, StackSettings)}
         *
         * @param stackSettings the stack settings
         * @return the builder
         */
        public Builder stackSettings(Map<String, StackSettings> stackSettings) {
            this.stackSettings = stackSettings;
            return this;
        }

        /**
         * Builds the {@link LocalWorkspaceOptions}.
         *
         * @return the options
         */
        public LocalWorkspaceOptions build() {
            return new LocalWorkspaceOptions(this);
        }
    }
}
