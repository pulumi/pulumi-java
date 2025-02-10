// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gson.reflect.TypeToken;

import com.pulumi.Context;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

/**
 * LocalWorkspace is a default implementation of the Workspace interface.
 * <p>
 * A Workspace is the execution context containing a single Pulumi project, a
 * program, and multiple stacks. Workspaces are used to manage the execution
 * environment, providing various utilities such as plugin installation,
 * environment configuration ($PULUMI_HOME), and creation, deletion, and listing
 * of Stacks.
 * <p>
 * LocalWorkspace relies on Pulumi.yaml and Pulumi.{stack}.yaml as the
 * intermediate format for Project and Stack settings. Modifying ProjectSettings
 * will alter the Workspace Pulumi.yaml file, and setting config on a Stack will
 * modify the Pulumi.{stack}.yaml file. This is identical to the behavior of
 * Pulumi CLI driven workspaces.
 * <p>
 * If not provided a working directory - causing LocalWorkspace to create a temp
 * directory, then the temp directory will be cleaned up when {@link #close()}
 * is called.
 */
public final class LocalWorkspace extends Workspace {
    private static final String[] SETTINGS_EXTENSIONS = { ".yaml", ".yml", ".json" };

    private final LocalSerializer serializer = new LocalSerializer();
    private final Path workDir;
    private final boolean ownsWorkingDir;
    @Nullable
    private final Path pulumiHome;
    @Nullable
    private final String secretsProvider;
    @Nullable
    private final Consumer<Context> program;
    @Nullable
    private final Logger logger;
    private final Map<String, String> environmentVariables;

    private LocalWorkspace(PulumiCommand cmd, LocalWorkspaceOptions options) throws AutomationException {
        super(cmd);

        Path dir = null;

        if (options != null) {
            if (options.workDir() != null) {
                dir = options.workDir();
            }

            this.pulumiHome = options.pulumiHome();
            this.program = options.program();
            this.logger = options.logger();
            this.secretsProvider = options.secretsProvider();
            this.environmentVariables = options.environmentVariables();
        } else {
            this.pulumiHome = null;
            this.program = null;
            this.logger = null;
            this.secretsProvider = null;
            this.environmentVariables = Collections.emptyMap();
        }

        if (dir == null || dir.toString().isBlank()) {
            try {
                dir = Files.createTempDirectory("automation-");
                this.ownsWorkingDir = true;
            } catch (IOException e) {
                throw new AutomationException(e);
            }
        } else {
            this.ownsWorkingDir = false;
        }

        this.workDir = dir;

        if (options != null) {
            var projectSettings = options.projectSettings();
            if (projectSettings != null) {
                initializeProjectSettings(projectSettings);
            }

            var stackSettings = options.stackSettings();
            if (stackSettings != null) {
                for (var entry : stackSettings.entrySet()) {
                    saveStackSettings(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void initializeProjectSettings(ProjectSettings settings) throws AutomationException {
        // If given project settings, we want to write them out to
        // the working dir. We do not want to override existing
        // settings with default settings though.

        var existingSettings = getProjectSettings();
        if (!existingSettings.isPresent()) {
            saveProjectSettings(settings);
        } else if (!existingSettings.get().isDefault() && !existingSettings.get().equals(settings)) {
            var path = findSettingsFile();
            throw new ProjectSettingsConflictException(path.toString());
        }
    }

    /**
     * Creates a workspace.
     *
     * @return the workspace
     * @throws AutomationException if an error occurs
     */
    public static LocalWorkspace create() throws AutomationException {
        return create(null);
    }

    /**
     * Creates a workspace using the specified options. Used for maximal control and
     * customization of the underlying environment before any stacks are created or
     * selected.
     *
     * @param options Options used to configure the workspace
     * @return the workspace
     * @throws AutomationException if an error occurs
     */
    public static LocalWorkspace create(@Nullable LocalWorkspaceOptions options) throws AutomationException {
        var cmd = getOrCreatePulumiCommand(options);
        return new LocalWorkspace(cmd, options);
    }

    private static PulumiCommand getOrCreatePulumiCommand(
            @Nullable LocalWorkspaceOptions options) throws AutomationException {
        return options != null && options.pulumiCommand() != null
                ? options.pulumiCommand()
                : LocalPulumiCommand.create(LocalPulumiCommandOptions.builder()
                        .skipVersionCheck(optOutOfVersionCheck(options != null
                                ? options.environmentVariables()
                                : null))
                        .build());
    }

    private static boolean optOutOfVersionCheck(@Nullable Map<String, String> environmentVariables) {
        var hasSkipEnvVar = environmentVariables != null &&
                environmentVariables.containsKey(LocalPulumiCommand.SKIP_VERSION_CHECK_VAR);
        var optOut = hasSkipEnvVar || System.getenv(LocalPulumiCommand.SKIP_VERSION_CHECK_VAR) != null;
        return optOut;
    }

    /**
     * Creates a stack with a {@link LocalWorkspace} utilizing the specified inline
     * (in process) {@code program}. This program is fully debuggable and runs in
     * process. Default project settings will be created on behalf of the user and
     * the working directory will default to a new temporary directory provided by
     * the OS.
     *
     * @param projectName the name of the project
     * @param stackName   the name of the stack
     * @param program     the program to run
     * @return the stack
     * @throws StackAlreadyExistsException if a stack with the provided name already
     *                                     exists
     * @throws AutomationException         if an error occurs
     */
    public static WorkspaceStack createStack(
            String projectName,
            String stackName,
            Consumer<Context> program) throws AutomationException {
        return createStack(projectName, stackName, program, LocalWorkspaceOptions.EMPTY);
    }

    /**
     * Creates a stack with a {@link LocalWorkspace} utilizing the specified inline
     * (in process) {@code program}. This program is fully debuggable and runs in
     * process. If no {@link LocalWorkspaceOptions#projectSettings()} option is
     * specified, default project settings will be created on behalf of the user.
     * Similarly, unless a {@link LocalWorkspaceOptions#workDir()} option is
     * specified, the working directory will default to a new temporary directory
     * provided by the OS.
     *
     * @param projectName the name of the project
     * @param stackName   the name of the stack
     * @param program     the program to run
     * @param options     options used to configure the workspace
     * @return the stack
     * @throws StackAlreadyExistsException if a stack with the provided name already
     *                                     exists
     * @throws AutomationException         if an error occurs
     */
    public static WorkspaceStack createStack(
            String projectName,
            String stackName,
            Consumer<Context> program,
            LocalWorkspaceOptions options) throws AutomationException {
        return createStackHelper(projectName, stackName, program, options, WorkspaceStack::create);
    }

    /**
     * Creates a Stack with a {@link LocalWorkspace} utilizing the local Pulumi CLI
     * program from the specified {@code workDir}. This is a way to create drivers
     * on top of pre-existing Pulumi programs. This Workspace will pick up any
     * available Settings files(Pulumi.yaml, Pulumi.{stack}.yaml).
     *
     * @param stackName the name of the stack
     * @param workDir   the working directory
     * @return the stack
     * @throws StackAlreadyExistsException if a stack with the provided name already
     *                                     exists
     * @throws AutomationException         if an error occurs
     */
    public static WorkspaceStack createStack(
            String stackName,
            Path workDir) throws AutomationException {
        return createStack(stackName, workDir, LocalWorkspaceOptions.EMPTY);
    }

    /**
     * Creates a Stack with a {@link LocalWorkspace} utilizing the local Pulumi CLI
     * program from the specified {@code workDir}. This is a way to create drivers
     * on top of pre-existing Pulumi programs. This Workspace will pick up any
     * available Settings files(Pulumi.yaml, Pulumi.{stack}.yaml).
     *
     * @param stackName the name of the stack
     * @param workDir   the working directory
     * @param options   options used to configure the workspace
     * @return the stack
     * @throws StackAlreadyExistsException if a stack with the provided name already
     *                                     exists
     * @throws AutomationException         if an error occurs
     */
    public static WorkspaceStack createStack(
            String stackName,
            Path workDir,
            LocalWorkspaceOptions options) throws AutomationException {
        return createStackHelper(stackName, workDir, options, WorkspaceStack::create);
    }

    /**
     * Selects an existing Stack with a {@link LocalWorkspace} utilizing the
     * specified inline (in process) {@code program}. This program is fully
     * debuggable and runs in process. Default project settings will be created on
     * behalf of the user and the working directory will default to a new temporary
     * directory provided by the OS.
     *
     * @param projectName the name of the project
     * @param stackName   the name of the stack
     * @param program     the program to run
     * @return the stack
     * @throws StackNotFoundException if a stack with the provided name does not
     *                                exist
     * @throws AutomationException    if an error occurs
     */
    public static WorkspaceStack selectStack(
            String projectName,
            String stackName,
            Consumer<Context> program) throws AutomationException {
        return selectStack(projectName, stackName, program, LocalWorkspaceOptions.EMPTY);
    }

    /**
     * Selects an existing Stack with a {@link LocalWorkspace} utilizing the
     * specified inline (in process) {@code program}. This program is fully
     * debuggable and runs in process. If no
     * {@link LocalWorkspaceOptions#projectSettings()} option is specified,
     * default project settings will be created on behalf of the user. Similarly,
     * unless a {@link LocalWorkspaceOptions#workDir()} option is specified, the
     * working directory will default to a new temporary directory provided by the
     * OS.
     *
     * @param projectName the name of the project
     * @param stackName   the name of the stack
     * @param program     the program to run
     * @param options     options used to configure the workspace
     * @return the stack
     * @throws StackNotFoundException if a stack with the provided name does not
     *                                exist
     * @throws AutomationException    if an error occurs
     */
    public static WorkspaceStack selectStack(
            String projectName,
            String stackName,
            Consumer<Context> program,
            LocalWorkspaceOptions options) throws AutomationException {
        return createStackHelper(projectName, stackName, program, options, WorkspaceStack::select);
    }

    /**
     * Selects an existing Stack with a {@link LocalWorkspace} utilizing the local
     * Pulumi CLI program from the specified {@code workDir}. This is a way to
     * create drivers on top of pre-existing Pulumi programs. This Workspace will
     * pick up any available Settings files(Pulumi.yaml, Pulumi.{stack}.yaml).
     *
     * @param stackName the name of the stack
     * @param workDir   the working directory
     * @return the stack
     * @throws StackNotFoundException if a stack with the provided name does not
     *                                exist
     * @throws AutomationException    if an error occurs
     */
    public static WorkspaceStack selectStack(
            String stackName,
            Path workDir) throws AutomationException {
        return selectStack(stackName, workDir, LocalWorkspaceOptions.EMPTY);
    }

    /**
     * Selects an existing Stack with a {@link LocalWorkspace} utilizing the local
     * Pulumi CLI program from the specified {@code workDir}. This is a way to
     * create drivers on top of pre-existing Pulumi programs. This Workspace will
     * pick up any available Settings files(Pulumi.yaml, Pulumi.{stack}.yaml).
     *
     * @param stackName the name of the stack
     * @param workDir   the working directory
     * @param options   options used to configure the workspace
     * @return the stack
     * @throws StackNotFoundException if a stack with the provided name does not
     *                                exist
     * @throws AutomationException    if an error occurs
     */
    public static WorkspaceStack selectStack(
            String stackName,
            Path workDir,
            LocalWorkspaceOptions options) throws AutomationException {
        return createStackHelper(stackName, workDir, options, WorkspaceStack::select);
    }

    /**
     * Creates or selects an existing Stack with a {@link LocalWorkspace} utilizing
     * the specified inline (in process) {@code program}. This program is fully
     * debuggable and runs in process. Default project settings will be created on
     * behalf of the user and the working directory will default to a new temporary
     * directory provided by the OS.
     *
     * @param projectName the name of the project
     * @param stackName   the name of the stack
     * @param program     the program to run
     * @return the stack
     * @throws AutomationException if an error occurs
     */
    public static WorkspaceStack createOrSelectStack(
            String projectName,
            String stackName,
            Consumer<Context> program) throws AutomationException {
        return createOrSelectStack(projectName, stackName, program, LocalWorkspaceOptions.EMPTY);
    }

    /**
     * Creates or selects an existing Stack with a {@link LocalWorkspace} utilizing
     * the specified inline (in process) {@code program}. This program is fully
     * debuggable and runs in process. If no
     * {@link LocalWorkspaceOptions#projectSettings()} option is specified,
     * default project settings will be created on behalf of the user. Similarly,
     * unless a {@link LocalWorkspaceOptions#workDir()} option is specified, the
     * working directory will default to a new temporary directory provided by the
     * OS.
     *
     * @param projectName the name of the project
     * @param stackName   the name of the stack
     * @param program     the program to run
     * @param options     options used to configure the workspace
     * @return the stack
     * @throws AutomationException if an error occurs
     */
    public static WorkspaceStack createOrSelectStack(
            String projectName,
            String stackName,
            Consumer<Context> program,
            LocalWorkspaceOptions options) throws AutomationException {
        return createStackHelper(projectName, stackName, program, options, WorkspaceStack::createOrSelect);
    }

    /**
     * Creates or selects an existing Stack with a {@link LocalWorkspace} utilizing
     * the local Pulumi CLI program from the specified {@code workDir}. This is a
     * way to create drivers on top of pre-existing Pulumi programs. This Workspace
     * will pick up any available Settings files(Pulumi.yaml, Pulumi.{stack}.yaml).
     *
     * @param stackName the name of the stack
     * @param workDir   the working directory
     * @return the stack
     * @throws AutomationException if an error occurs
     */
    public static WorkspaceStack createOrSelectStack(
            String stackName,
            Path workDir) throws AutomationException {
        return createOrSelectStack(stackName, workDir, LocalWorkspaceOptions.EMPTY);
    }

    /**
     * Creates or selects an existing Stack with a {@link LocalWorkspace} utilizing
     * the local Pulumi CLI program from the specified {@code workDir}. This is a
     * way to create drivers on top of pre-existing Pulumi programs. This Workspace
     * will pick up any available Settings files(Pulumi.yaml, Pulumi.{stack}.yaml).
     *
     * @param stackName the name of the stack
     * @param workDir   the working directory
     * @param options   options used to configure the workspace
     * @return the stack
     * @throws AutomationException if an error occurs
     */
    public static WorkspaceStack createOrSelectStack(
            String stackName,
            Path workDir,
            LocalWorkspaceOptions options) throws AutomationException {
        return createStackHelper(stackName, workDir, options, WorkspaceStack::createOrSelect);
    }

    private static WorkspaceStack createStackHelper(
            String projectName,
            String stackName,
            Consumer<Context> program,
            LocalWorkspaceOptions options,
            WorkspaceStackFactory factory) throws AutomationException {

        var cmd = getOrCreatePulumiCommand(options);
        options = options != null ? options : LocalWorkspaceOptions.EMPTY;
        options = options.forInlineProgram(program, ProjectSettings.createDefault(projectName));
        var ws = new LocalWorkspace(cmd, options);
        return factory.create(stackName, ws);
    }

    private static WorkspaceStack createStackHelper(
            String stackName,
            Path workDir,
            LocalWorkspaceOptions options,
            WorkspaceStackFactory factory) throws AutomationException {

        var cmd = getOrCreatePulumiCommand(options);
        options = options != null ? options : LocalWorkspaceOptions.EMPTY;
        options = options.forLocalProgram(workDir);
        var ws = new LocalWorkspace(cmd, options);
        return factory.create(stackName, ws);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path workDir() {
        return workDir;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Path pulumiHome() {
        return pulumiHome;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String pulumiVersion() {
        var version = cmd.version();
        if (version == null) {
            throw new IllegalStateException("Failed to get Pulumi version");
        }
        return version.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public String secretsProvider() {
        return secretsProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Consumer<Context> program() {
        return program;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Logger logger() {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> environmentVariables() {
        return environmentVariables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ProjectSettings> getProjectSettings() throws AutomationException {
        try {
            var path = findSettingsFile();
            if (!Files.exists(path)) {
                return Optional.empty();
            }
            var content = Files.readString(path);
            var ext = getPathExtension(path);
            return ".json".equals(ext)
                    ? Optional.of(serializer.deserializeJson(content, ProjectSettings.class))
                    : Optional.of(serializer.deserializeYaml(content, ProjectSettings.class));
        } catch (IOException e) {
            throw new AutomationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProjectSettings(ProjectSettings settings) throws AutomationException {
        var path = findSettingsFile();
        var ext = getPathExtension(path);
        var content = ".json".equals(ext)
                ? serializer.serializeJson(settings)
                : serializer.serializeYaml(settings);
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new AutomationException("Error reading project settings at " + path, e);
        }
    }

    private static String getPathExtension(Path path) {
        var filename = path.getFileName().toString();
        return filename.contains(".")
                ? filename.substring(filename.lastIndexOf("."))
                : "";
    }

    private Path findSettingsFile() {
        for (String ext : SETTINGS_EXTENSIONS) {
            var testPath = workDir.resolve("Pulumi" + ext);
            if (Files.exists(testPath)) {
                return testPath;
            }
        }
        return workDir.resolve("Pulumi.yaml");
    }

    private static String getStackSettingsName(String stackName) {
        var parts = stackName.split("/");
        if (parts.length < 1) {
            return stackName;
        }
        return parts[parts.length - 1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<StackSettings> getStackSettings(String stackName) throws AutomationException {
        var settingsName = getStackSettingsName(Objects.requireNonNull(stackName));

        for (var ext : SETTINGS_EXTENSIONS) {
            var path = workDir.resolve("Pulumi." + settingsName + ext);
            if (!Files.exists(path)) {
                continue;
            }

            try {
                var content = Files.readString(path);
                return ".json".equals(ext)
                        ? Optional.of(serializer.deserializeJson(content, StackSettings.class))
                        : Optional.of(serializer.deserializeYaml(content, StackSettings.class));
            } catch (Exception e) {
                throw new AutomationException("Error reading stack settings at " + path, e);
            }
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveStackSettings(String stackName, StackSettings settings) throws AutomationException {
        var settingsName = getStackSettingsName(Objects.requireNonNull(stackName));

        var foundExt = ".yaml";
        for (var ext : SETTINGS_EXTENSIONS) {
            var testPath = workDir.resolve("Pulumi." + settingsName + ext);
            if (Files.exists(testPath)) {
                foundExt = ext;
                break;
            }
        }
        var path = workDir.resolve("Pulumi." + settingsName + foundExt);

        try {
            var content = ".json".equals(foundExt)
                    ? serializer.serializeJson(settings)
                    : serializer.serializeYaml(settings);
            Files.writeString(path, content);
        } catch (Exception e) {
            throw new AutomationException("Error saving stack settings at " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> serializeArgsForOp(String stackName) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postCommandCallback(String stackName) throws AutomationException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhoAmIResult whoAmI() throws AutomationException {
        var args = List.of("whoami", "--json");
        var result = runCommand(args);
        return serializer.deserializeJson(result.standardOutput(), WhoAmIResult.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createStack(String stackName) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("stack");
        args.add("init");
        args.add(Objects.requireNonNull(stackName));

        if (this.secretsProvider != null && !this.secretsProvider.isBlank()) {
            args.add("--secrets-provider");
            args.add(this.secretsProvider);
        }

        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectStack(String stackName) throws AutomationException {
        var args = List.of("stack", "select", "--stack", Objects.requireNonNull(stackName));
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeStack(String stackName) throws AutomationException {
        var args = List.of("stack", "rm", "--yes", Objects.requireNonNull(stackName));
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StackSummary> listStacks() throws AutomationException {
        var args = List.of("stack", "ls", "--json");
        var result = runCommand(args);
        if (result.standardOutput().isBlank()) {
            return Collections.emptyList();
        }

        var listType = new TypeToken<List<StackSummary>>() {
        }.getType();
        return serializer.deserializeJson(result.standardOutput(), listType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackDeployment exportStack(String stackName) throws AutomationException {
        var args = List.of("stack", "export", "--stack", Objects.requireNonNull(stackName), "--show-secrets");
        var result = runCommand(args);
        return StackDeployment.fromJson(result.standardOutput());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importStack(String stackName, StackDeployment state) throws AutomationException {
        try {
            var json = serializer.serializeJson(Objects.requireNonNull(state));

            var tempFile = Files.createTempFile("stack", ".json");
            try {
                Files.writeString(tempFile, json);
                var args = List.of("stack", "import", "--file", tempFile.toString(), "--stack",
                        Objects.requireNonNull(stackName));
                runCommand(args);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (AutomationException e) {
            throw e;
        } catch (Exception e) {
            throw new AutomationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEnvironments(String stackName, Collection<String> environments) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("config");
        args.add("env");
        args.add("add");
        args.add("--stack");
        args.add(Objects.requireNonNull(stackName));
        args.add("--yes");
        args.addAll(Objects.requireNonNull(environments));
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeEnvironment(String stackName, String environment) throws AutomationException {
        var args = List.of("config", "env", "rm", Objects.requireNonNull(environment), "--stack",
                Objects.requireNonNull(stackName), "--yes");
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTag(String stackName, String key) throws AutomationException {
        var args = List.of("stack", "tag", "get", Objects.requireNonNull(key), "--stack",
                Objects.requireNonNull(stackName));
        var result = runCommand(args);
        return result.standardOutput().trim();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTag(String stackName, String key, String value) throws AutomationException {
        var args = List.of("stack", "tag", "set", Objects.requireNonNull(key), Objects.requireNonNull(value), "--stack",
                Objects.requireNonNull(stackName));
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeTag(String stackName, String key) throws AutomationException {
        var args = List.of("stack", "tag", "rm", Objects.requireNonNull(key), "--stack",
                Objects.requireNonNull(stackName));
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> listTags(String stackName) throws AutomationException {
        var args = List.of("stack", "tag", "ls", "--json", "--stack", Objects.requireNonNull(stackName));
        var result = runCommand(args);

        var mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        return serializer.deserializeJson(result.standardOutput(), mapType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigValue getConfig(String stackName, String key, boolean path) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("config");
        args.add("get");
        if (path) {
            args.add("--path");
        }
        args.add(Objects.requireNonNull(key));
        args.add("--json");
        args.add("--stack");
        args.add(Objects.requireNonNull(stackName));
        var result = runCommand(args);

        return serializer.deserializeJson(result.standardOutput(), ConfigValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ConfigValue> getAllConfig(String stackName) throws AutomationException {
        var args = List.of("config", "--show-secrets", "--json", "--stack", Objects.requireNonNull(stackName));
        var result = runCommand(args);
        if (result.standardOutput().isBlank()) {
            return Collections.emptyMap();
        }
        var mapType = new TypeToken<Map<String, ConfigValue>>() {
        }.getType();
        return serializer.deserializeJson(result.standardOutput(), mapType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(String stackName, String key, ConfigValue value, boolean path) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("config");
        args.add("set");
        if (path) {
            args.add("--path");
        }
        args.add(Objects.requireNonNull(key));
        Objects.requireNonNull(value);
        var secretArg = value.isSecret() ? "--secret" : "--plaintext";
        args.add(secretArg);
        args.add("--stack");
        args.add(Objects.requireNonNull(stackName));
        args.add("--non-interactive");
        args.add("--");
        args.add(value.value());
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllConfig(String stackName, Map<String, ConfigValue> configMap, boolean path)
            throws AutomationException {
        var args = new ArrayList<String>();
        args.add("config");
        args.add("set-all");
        args.add("--stack");
        args.add(Objects.requireNonNull(stackName));

        if (path) {
            args.add("--path");
        }

        for (var entry : configMap.entrySet()) {
            String secretArg = entry.getValue().isSecret() ? "--secret" : "--plaintext";
            args.add(secretArg);
            args.add(entry.getKey() + "=" + entry.getValue().value());
        }

        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeConfig(String stackName, String key, boolean path) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("config");
        args.add("rm");
        args.add(Objects.requireNonNull(key));
        args.add("--stack");
        args.add(Objects.requireNonNull(stackName));
        if (path) {
            args.add("--path");
        }
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllConfig(String stackName, Collection<String> keys, boolean path) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("config");
        args.add("rm-all");
        args.add("--stack");
        args.add(Objects.requireNonNull(stackName));
        if (path) {
            args.add("--path");
        }
        args.addAll(Objects.requireNonNull(keys));
        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ConfigValue> refreshConfig(String stackName) throws AutomationException {
        var args = List.of("config", "refresh", "--force", "--stack", Objects.requireNonNull(stackName));
        runCommand(args);
        return getAllConfig(stackName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installPlugin(String name, String version, PluginInstallOptions options) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("plugin");
        args.add("install");

        var kind = options != null ? options.kind() : PluginKind.RESOURCE;
        args.add(kind.toString().toLowerCase());

        args.add(Objects.requireNonNull(name));
        args.add(Objects.requireNonNull(version));

        if (options != null) {
            if (options.isExactVersion()) {
                args.add("--exact");
            }

            var serverUrl = options.serverUrl();
            if (serverUrl != null && !serverUrl.isBlank()) {
                args.add("--server");
                args.add(serverUrl);
            }
        }

        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePlugin(PluginRemoveOptions options) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("plugin");
        args.add("rm");

        var kind = options != null ? options.kind() : PluginKind.RESOURCE;
        args.add(kind.toString().toLowerCase());

        if (options != null) {
            var name = options.name();
            if (name != null && !name.isBlank()) {
                args.add(name);
            }

            var versionRange = options.versionRange();
            if (versionRange != null && !versionRange.isBlank()) {
                args.add(versionRange);
            }
        }

        args.add("--yes");

        runCommand(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PluginInfo> listPlugins() throws AutomationException {
        var args = List.of("plugin", "ls", "--json");
        var result = runCommand(args);

        var listType = new TypeToken<List<PluginInfo>>() {
        }.getType();
        return serializer.deserializeJson(result.standardOutput(), listType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, OutputValue> getStackOutputs(String stackName) throws AutomationException {
        Objects.requireNonNull(stackName);
        // Note: https://github.com/pulumi/pulumi/issues/6050 is closed, so we
        // could try running these commands in parallel to speed up the operation.
        var maskedResult = runCommand(List.of("stack", "output", "--json", "--stack", stackName));
        var plaintextResult = runCommand(List.of("stack", "output", "--json", "--show-secrets", "--stack", stackName));

        var maskedStdout = maskedResult.standardOutput().trim();
        var plaintextStdout = plaintextResult.standardOutput().trim();

        var type = new TypeToken<Map<String, String>>() {
        }.getType();

        Map<String, String> maskedOutput = maskedStdout.isEmpty()
                ? Collections.emptyMap()
                : serializer.deserializeJson(maskedStdout, type);

        Map<String, String> plaintextOutput = plaintextStdout.isEmpty()
                ? Collections.emptyMap()
                : serializer.deserializeJson(plaintextStdout, type);

        var output = new HashMap<String, OutputValue>();
        for (var entry : plaintextOutput.entrySet()) {
            var isSecret = maskedOutput.get(entry.getKey()).equals("[secret]");
            output.put(entry.getKey(), new OutputValue(entry.getValue(), isSecret));
        }
        return Collections.unmodifiableMap(output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeSecretsProvider(String stackName, String newSecretsProvider,
            @Nullable SecretsProviderOptions options) throws AutomationException {
        var args = List.of("stack", "change-secrets-provider", "--stack", Objects.requireNonNull(stackName),
                Objects.requireNonNull(newSecretsProvider));
        var builder = CommandRunOptions.builder();

        if (newSecretsProvider.equals("passphrase")) {
            var message = "New passphrase must be set when using passphrase provider";
            Objects.requireNonNull(options, message);
            var newPassphrase = options.newPassphrase();
            Objects.requireNonNull(newPassphrase, message);
            if (newPassphrase.isEmpty()) {
                throw new IllegalArgumentException(message);
            }
            builder.standardInput(newPassphrase);
        }

        runCommand(args, builder.build());
    }

    @Override
    public void close() throws Exception {
        if (this.ownsWorkingDir && Files.exists(this.workDir)) {
            try {
                Files.walk(this.workDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Silently ignore
                            }
                        });
            } catch (Exception e) {
                // allow graceful exit if for some reason
                // we're not able to delete the directory
                // will rely on OS to clean temp directory
                // in this case.
            }
        }
    }

    // private static boolean optOutOfVersionCheck(Map<String, String>
    // environmentVariables) {
    // boolean hasSkipEnvVar = environmentVariables != null &&
    // environmentVariables.containsKey("PULUMI_SKIP");
    // boolean optOut = hasSkipEnvVar || System.getenv("PULUMI_SKIP") != null;
    // return optOut;
    // }

    @FunctionalInterface
    private interface WorkspaceStackFactory {
        WorkspaceStack create(String name, Workspace workspace) throws AutomationException;
    }
}
