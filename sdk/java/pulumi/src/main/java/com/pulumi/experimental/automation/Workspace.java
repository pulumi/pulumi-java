// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.pulumi.Context;

/**
 * Workspace is the execution context containing a single Pulumi project, a
 * program, and multiple stacks.
 * <p>
 * Workspaces are used to manage the execution environment, providing various
 * utilities such as plugin installation, environment configuration
 * ($PULUMI_HOME), and creation, deletion, and listing of Stacks.
 */
public abstract class Workspace implements AutoCloseable {
    final PulumiCommand cmd;

    Workspace(PulumiCommand cmd) {
        this.cmd = cmd;
    }

    /**
     * The working directory to run Pulumi CLI commands.
     *
     * @return the working directory
     */
    public abstract Path workDir();

    /**
     * The directory override for CLI metadata if set.
     * <p>
     * This customizes the location of $PULUMI_HOME where metadata is stored and
     * plugins are installed.
     *
     * @return the directory override
     */
    @Nullable
    public abstract Path pulumiHome();

    /**
     * The version of the underlying Pulumi CLI/Engine.
     *
     * @return the version
     */
    public abstract String pulumiVersion();

    /**
     * The secrets provider to use for encryption and decryption of stack secrets.
     * <p>
     * See:
     * https://www.pulumi.com/docs/intro/concepts/secrets/#available-encryption-providers
     *
     * @return the secrets provider
     */
    @Nullable
    public abstract String secretsProvider();

    /**
     * The inline program to be used for Preview/Update operations if any.
     * <p>
     * If non specified, the stack will refer to {@link ProjectSettings} for this
     * information.
     *
     * @return the inline program
     */
    @Nullable
    public abstract Consumer<Context> program();

    /**
     * A custom logger instance that will be used for the action. Note that it will
     * only be used
     * {@link Workspace#program()} is also provided.
     *
     * @return The logger
     */
    @Nullable
    public abstract Logger logger();

    /**
     * Environment values scoped to the current workspace. These will be supplied to
     * every Pulumi command.
     *
     * @return the environment variables
     */
    public abstract Map<String, String> environmentVariables();

    /**
     * Returns project settings for the current project if any.
     *
     * @return the project settings
     * @throws AutomationException if there was an issue retrieving the project
     */
    public abstract Optional<ProjectSettings> getProjectSettings() throws AutomationException;

    /**
     * Overwrites the settings for the current project.
     * <p>
     * There can only be a single project per workspace. Fails if new project name
     * does not match old.
     *
     * @param settings the settings object to save
     * @throws AutomationException if there was an issue saving the project
     */
    public abstract void saveProjectSettings(ProjectSettings settings) throws AutomationException;

    /**
     * Returns stack settings for the stack matching the specified stack name if
     * any.
     *
     * @param stackName the name of the stack
     * @return the stack settings
     * @throws AutomationException if there was an issue retrieving the stack
     */
    public abstract Optional<StackSettings> getStackSettings(String stackName) throws AutomationException;

    /**
     * Overwrite the settings for the stack matching the specified stack name.
     *
     * @param stackName the name of the stack to operation on
     * @param settings  the settings object to save
     * @throws AutomationException if there was an issue saving the stack
     */
    public abstract void saveStackSettings(String stackName, StackSettings settings) throws AutomationException;

    /**
     * Hook to provide additional args to every CLI command before they are
     * executed.
     * <p>
     * Provided with a stack name, returns a list of args to append to an invoked
     * command ["--config=...", ].
     * <p>
     * {@link LocalWorkspace} does not utilize this extensibility point.
     *
     * @param stackName the name of the stack
     * @return the list of args to append
     */
    public abstract List<String> serializeArgsForOp(String stackName);

    /**
     * Hook executed after every command. Called with the stack name.
     * <p>
     * An extensibility point to perform workspace cleanup (CLI operations may
     * create/modify a Pulumi.stack.yaml).
     * <p>
     * {@link LocalWorkspace} does not utilize this extensibility point.
     *
     * @param stackName the name of the stack
     * @throws AutomationException if there was an issue executing the post command
     */
    public abstract void postCommandCallback(String stackName) throws AutomationException;

    /**
     * Adds environments to the end of a stack's import list. Imported environments
     * are merged in order per the ESC merge rules. The list of environments behaves
     * as if it were the import list in an anonymous environment.
     *
     * @param stackName    the name of the stack
     * @param environments list of environments to add to the end of the stack's
     *                     import list
     * @throws AutomationException if there was an issue adding the environments
     */
    public abstract void addEnvironments(String stackName, Collection<String> environments) throws AutomationException;

    /**
     * Removes environments from a stack's import list.
     *
     * @param stackName   the name of the stack
     * @param environment the name of the environment to remove from the stack's
     *                    configuration
     * @throws AutomationException if there was an issue removing the environment
     */
    public abstract void removeEnvironment(String stackName, String environment) throws AutomationException;

    /**
     * Returns the value associated with the stack and key, scoped to the Workspace.
     *
     * @param stackName the name of the stack to read tag metadata from
     * @param key       the key to use for the tag lookup
     * @return the value associated with the key
     * @throws AutomationException if there was an issue reading the tag
     */
    public abstract String getTag(String stackName, String key) throws AutomationException;

    /**
     * Sets the specified key-value pair on the provided stack name.
     *
     * @param stackName the stack to operate on
     * @param key       the tag key to set
     * @param value     the tag value to set
     * @throws AutomationException if there was an issue setting the tag
     */
    public abstract void setTag(String stackName, String key, String value) throws AutomationException;

    /**
     * Removes the specified key-value pair on the provided stack name.
     *
     * @param stackName the stack to operate on
     * @param key       the tag key to remove
     * @throws AutomationException if there was an issue removing the tag
     */
    public abstract void removeTag(String stackName, String key) throws AutomationException;

    /**
     * Returns the tag map for the specified stack name, scoped to the current
     * Workspace.
     *
     * @param stackName the stack to operate on
     * @return the tag map for the specified stack name
     * @throws AutomationException if there was an issue listing the tags
     */
    public abstract Map<String, String> listTags(String stackName) throws AutomationException;

    /**
     * Returns the value associated with the specified stack name and key, scoped to
     * the Workspace.
     *
     * @param stackName the name of the stack to read config from
     * @param key       the key to use for the config lookup
     * @return the value associated with the key
     * @throws AutomationException if there was an issue reading the config
     */
    public ConfigValue getConfig(String stackName, String key) throws AutomationException {
        return getConfig(stackName, key, false);
    }

    /**
     * Returns the value associated with the specified stack name and key, scoped to
     * the Workspace.
     *
     * @param stackName the name of the stack to read config from
     * @param key       the key to use for the config lookup
     * @param path      the key contains a path to a property in a map or list to
     *                  get
     * @return the value associated with the key
     * @throws AutomationException if there was an issue reading the config
     */
    public abstract ConfigValue getConfig(String stackName, String key, boolean path) throws AutomationException;

    /**
     * Returns the config map for the specified stack name, scoped to the current
     * Workspace.
     *
     * @param stackName the name of the stack to read config from
     * @return the config map for the specified stack name
     * @throws AutomationException if there was an issue listing the config
     */
    public abstract Map<String, ConfigValue> getAllConfig(String stackName) throws AutomationException;

    /**
     * Sets the specified key-value pair in the provided stack's config.
     *
     * @param stackName the name of the stack to operate on
     * @param key       the config key to set
     * @param value     the config value to set
     * @throws AutomationException if there was an issue setting the config
     */
    public void setConfig(String stackName, String key, ConfigValue value) throws AutomationException {
        setConfig(stackName, key, value, false);
    }

    /**
     * Sets the specified key-value pair in the provided stack's config.
     *
     * @param stackName the name of the stack to operate on
     * @param key       the config key to set
     * @param value     the config value to set
     * @param path      the key contains a path to a property in a map or list to
     *                  set
     * @throws AutomationException if there was an issue setting the config
     */
    public abstract void setConfig(String stackName, String key, ConfigValue value, boolean path)
            throws AutomationException;

    /**
     * Sets all values in the provided config map for the specified stack name.
     *
     * @param stackName the name of the stack to operate on
     * @param configMap the config map to upsert against the existing config
     * @throws AutomationException if there was an issue setting the config
     */
    public void setAllConfig(String stackName, Map<String, ConfigValue> configMap) throws AutomationException {
        setAllConfig(stackName, configMap, false);
    }

    /**
     * Sets all values in the provided config map for the specified stack name.
     *
     * @param stackName the name of the stack to operate on
     * @param configMap the config map to upsert against the existing config
     * @param path      the keys contain a path to a property in a map or list to
     *                  set
     * @throws AutomationException if there was an issue setting the config
     */
    public abstract void setAllConfig(String stackName, Map<String, ConfigValue> configMap, boolean path)
            throws AutomationException;

    /**
     * Removes the specified key-value pair from the provided stack's config.
     *
     * @param stackName the name of the stack to operate on
     * @param key       the config key to remove
     * @throws AutomationException if there was an issue removing the config
     */
    public void removeConfig(String stackName, String key) throws AutomationException {
        removeConfig(stackName, key, false);
    }

    /**
     * Removes the specified key-value pair from the provided stack's config.
     *
     * @param stackName the name of the stack to operate on
     * @param key       the config key to remove
     * @param path      the key contains a path to a property in a map or list to
     *                  remove
     * @throws AutomationException if there was an issue removing the config
     */
    public abstract void removeConfig(String stackName, String key, boolean path) throws AutomationException;

    /**
     * Removes all values in the provided key collection from the config map for the
     * specified stack name.
     *
     * @param stackName the name of the stack to operate on
     * @param keys      the collection of keys to remove from the underlying config
     *                  map
     * @throws AutomationException if there was an issue removing the config
     */
    public void removeAllConfig(String stackName, Collection<String> keys) throws AutomationException {
        removeAllConfig(stackName, keys, false);
    }

    /**
     * Removes all values in the provided key collection from the config map for the
     * specified stack name.
     *
     * @param stackName the name of the stack to operate on
     * @param keys      the collection of keys to remove from the underlying config
     *                  map
     * @param path      the keys contain a path to a property in a map or list to
     *                  remove
     * @throws AutomationException if there was an issue removing the config
     */
    public abstract void removeAllConfig(String stackName, Collection<String> keys, boolean path)
            throws AutomationException;

    /**
     * Gets and sets the config map used with the last update for the stack matching
     * the specified stack name.
     *
     * @param stackName the name of the stack to operate on
     * @return the config map used with the last update for the stack
     * @throws AutomationException if there was an issue refreshing the config
     */
    public abstract Map<String, ConfigValue> refreshConfig(String stackName) throws AutomationException;

    /**
     * Returns the currently authenticated user.
     *
     * @return the currently authenticated user
     * @throws AutomationException if there was an issue determining the current
     *                             user
     */
    public abstract WhoAmIResult whoAmI() throws AutomationException;

    /**
     * Returns a summary of the currently selected stack, if any.
     *
     * @return the summary of the currently selected stack
     * @throws AutomationException if there was an issue getting the stack
     */
    public Optional<StackSummary> getStack() throws AutomationException {
        var stacks = listStacks();
        return stacks.stream().filter(StackSummary::isCurrent).findFirst();
    }

    /**
     * Creates and sets a new stack with the specified stack name, failing if one
     * already exists.
     *
     * @param stackName the stack to create
     * @throws AutomationException if there was an issue creating the stack
     */
    public abstract void createStack(String stackName) throws AutomationException;

    /**
     * Selects and sets an existing stack matching the stack name, failing if none
     * exists.
     *
     * @param stackName the stack to select
     * @throws StackNotFoundException if the stack does not exist
     * @throws AutomationException    if there was an issue selecting the stack
     */
    public abstract void selectStack(String stackName) throws AutomationException;

    /**
     * Deletes the stack and all associated configuration and history.
     *
     * @param stackName the stack to remove
     * @throws AutomationException if there was an issue removing the stack
     */
    public abstract void removeStack(String stackName) throws AutomationException;

    /**
     * Returns all stacks created under the current project.
     * <p>
     * This queries underlying backend and may return stacks not present in the
     * Workspace (as Pulumi.{stack}.yaml files).
     *
     * @return the list of stacks
     * @throws AutomationException if there was an issue listing the stacks
     */
    public abstract List<StackSummary> listStacks() throws AutomationException;

    /**
     * Exports the deployment state of the stack.
     * <p>
     * This can be combined with {@link #importStack} to edit a
     * stack's state (such as recovery from failed deployments).
     *
     * @param stackName the stack to export
     * @return the deployment state of the stack
     * @throws AutomationException if there was an issue exporting the stack
     */
    public abstract StackDeployment exportStack(String stackName) throws AutomationException;

    /**
     * Imports the specified deployment state into a pre-existing stack.
     * <p>
     * This can be combined with {@link #exportStack} to edit a
     * stack's state (such as recovery from failed deployments).
     *
     * @param stackName the stack to import
     * @param state     the deployment state to import
     * @throws AutomationException if there was an issue importing the stack
     */
    public abstract void importStack(String stackName, StackDeployment state) throws AutomationException;

    /**
     * Installs a plugin in the Workspace, for example to use cloud providers like
     * AWS or GCP.
     *
     * @param name    the name of the plugin
     * @param version the version of the plugin, e.g. "v1.0.0"
     * @throws AutomationException if there was an issue installing the plugin
     */
    public void installPlugin(String name, String version) throws AutomationException {
        installPlugin(name, version, PluginInstallOptions.EMPTY);
    }

    /**
     * Installs a plugin in the Workspace, for example to use cloud providers like
     * AWS or GCP.
     *
     * @param name    the name of the plugin
     * @param version the version of the plugin, e.g. "v1.0.0"
     * @param options additional plugin installation options
     * @throws AutomationException if there was an issue installing the plugin
     */
    public abstract void installPlugin(String name, String version, PluginInstallOptions options)
            throws AutomationException;

    /**
     * Removes a plugin or plugins from the Workspace.
     *
     * @param options plugin removal options
     * @throws AutomationException if there was an issue removing the plugin
     */
    public abstract void removePlugin(PluginRemoveOptions options) throws AutomationException;

    /**
     * Returns a list of all plugins installed in the Workspace.
     *
     * @return the list of plugins
     * @throws AutomationException if there was an issue listing the plugins
     */
    public abstract List<PluginInfo> listPlugins() throws AutomationException;

    /**
     * Gets the current set of Stack outputs from the last {@link WorkspaceStack#up()}
     * call.
     *
     * @param stackName the name of the stack
     * @return the stack outputs
     * @throws AutomationException if there was an issue getting the stack outputs
     */
    public abstract Map<String, OutputValue> getStackOutputs(String stackName) throws AutomationException;

    /**
     * Change the secrets provider for a stack.
     *
     * @param stackName          the name of the stack
     * @param newSecretsProvider the new secrets provider
     * @throws AutomationException if there was an issue changing the secrets
     *                             provider
     */
    public void changeSecretsProvider(String stackName, String newSecretsProvider) throws AutomationException {
        changeSecretsProvider(stackName, newSecretsProvider, null);
    }

    /**
     * Change the secrets provider for a stack.
     *
     * @param stackName          the name of the stack
     * @param newSecretsProvider the new secrets provider
     * @param options            the options to change the secrets provider
     * @throws AutomationException if there was an issue changing the secrets
     *                             provider
     */
    public abstract void changeSecretsProvider(
            String stackName,
            String newSecretsProvider,
            @Nullable SecretsProviderOptions options) throws AutomationException;

    /**
     * Runs a Pulumi CLI command with the provided arguments.
     *
     * @param args the arguments to pass
     * @return the result of the command
     * @throws AutomationException if there was an issue running the command
     */
    CommandResult runCommand(List<String> args) throws AutomationException {
        return runCommand(args, CommandRunOptions.EMPTY);
    }

    /**
     * Runs a Pulumi CLI command with the provided arguments.
     *
     * @param args    the arguments to pass
     * @param options the options to run the command with
     * @return the result of the command
     * @throws AutomationException if there was an issue running the command
     */
    CommandResult runCommand(List<String> args, CommandRunOptions options) throws AutomationException {
        var env = new HashMap<String, String>();

        var pulumiHome = pulumiHome();
        if (pulumiHome != null) {
            var pulumiHomeStr = pulumiHome.toString();
            if (!pulumiHomeStr.isBlank()) {
                env.put("PULUMI_HOME", pulumiHomeStr);
            }
        }

        var envVars = environmentVariables();
        if (envVars != null) {
            env.putAll(envVars);
        }

        options = options.withAdditionalEnv(env);
        options = options.withWorkingDir(workDir());
        return cmd.run(args, options);
    }
}
