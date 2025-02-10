// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import com.google.gson.reflect.TypeToken;

import com.pulumi.Context;

import com.pulumi.experimental.automation.events.EngineEvent;
import com.pulumi.experimental.automation.events.SummaryEvent;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

/**
 * {@link WorkspaceStack} is an isolated, independently configurable instance of
 * a
 * Pulumi program. {@link WorkspaceStack} exposes methods for the full pulumi
 * lifecycle
 * (up/preview/refresh/destroy), as well as managing configuration.
 * <p>
 * Multiple stacks are commonly used to denote different phases of development
 * (such as development, staging, and production) or feature branches (such as
 * feature-x-dev, jane-feature-x-dev).
 * <p>
 * Will close the {@link Workspace} on {@link #close()}.
 */
public final class WorkspaceStack implements AutoCloseable {
    private final String name;
    private final Workspace workspace;
    private final WorkspaceStackState state;

    WorkspaceStack(String name, Workspace workspace, WorkspaceStackInitMode mode) throws AutomationException {
        this.name = Objects.requireNonNull(name);
        this.workspace = Objects.requireNonNull(workspace);
        this.state = new WorkspaceStackState(this);

        switch (mode) {
            case CREATE:
                workspace.createStack(name);
                break;

            case SELECT:
                workspace.selectStack(name);
                break;

            case CREATE_OR_SELECT:
                try {
                    workspace.selectStack(name);
                } catch (StackNotFoundException e) {
                    workspace.createStack(name);
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid WorkspaceStackInitMode: " + mode);
        }
    }

    /**
     * Gets the name identifying the Stack.
     *
     * @return the stack name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the Workspace the Stack was created from.
     *
     * @return the workspace
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Gets a module for editing the Stack's state.
     *
     * @return the module for editing the stack's state
     */
    public WorkspaceStackState getState() {
        return state;
    }

    /**
     * Creates a new stack using the given workspace, and stack name. It fails if a
     * stack with that name already exists.
     *
     * @param name      the name identifying the stack
     * @param workspace the workspace the stack was created from
     * @return the stack
     * @throws StackAlreadyExistsException if a stack with the provided name already
     *                                     exists
     * @throws AutomationException         if an error occurs
     */
    public static WorkspaceStack create(String name, Workspace workspace) throws AutomationException {
        return new WorkspaceStack(name, workspace, WorkspaceStackInitMode.CREATE);
    }

    /**
     * Selects stack using the given workspace, and stack name. It returns an error
     * if the given Stack does not exist.
     *
     * @param name      the name identifying the stack
     * @param workspace the workspace the stack was created from
     * @return the stack
     * @throws StackNotFoundException if a stack with the provided name does not
     *                                exist
     * @throws AutomationException    if an error occurs
     */
    public static WorkspaceStack select(String name, Workspace workspace) throws AutomationException {
        return new WorkspaceStack(name, workspace, WorkspaceStackInitMode.SELECT);
    }

    /**
     * Tries to create a new Stack using the given workspace, and stack name if the
     * stack does not already exist, or falls back to selecting an existing stack.
     * If the stack does not exist, it will be created and selected.
     *
     * @param name      the name identifying the stack
     * @param workspace the workspace the stack was created from
     * @return the stack
     * @throws AutomationException if an error occurs
     */
    public static WorkspaceStack createOrSelect(String name, Workspace workspace) throws AutomationException {
        return new WorkspaceStack(name, workspace, WorkspaceStackInitMode.CREATE_OR_SELECT);
    }

    /**
     * Returns the value associated with the stack and key, scoped to the Workspace.
     *
     * @param key the key to use for the tag lookup
     * @return the value associated with the key
     * @throws AutomationException if an error occurs
     */
    public String getTag(String key) throws AutomationException {
        return this.workspace.getTag(this.name, key);
    }

    /**
     * Sets the specified key-value pair on the stack.
     *
     * @param key   the tag key to set
     * @param value the tag value to set
     * @throws AutomationException if an error occurs
     */
    public void setTag(String key, String value) throws AutomationException {
        this.workspace.setTag(this.name, key, value);
    }

    /**
     * Removes the specified key-value pair on the provided stack name.
     *
     * @param key the tag key to remove
     * @throws AutomationException if an error occurs
     */
    public void removeTag(String key) throws AutomationException {
        this.workspace.removeTag(this.name, key);
    }

    /**
     * Returns the tag map for the specified stack name, scoped to the current
     * Workspace.
     *
     * @return the tag map for the specified stack name
     * @throws AutomationException if an error occurs
     */
    public Map<String, String> listTags() throws AutomationException {
        return this.workspace.listTags(this.name);
    }

    /**
     * Returns the config value associated with the specified key.
     *
     * @param key the key to use for the config lookup
     * @return the value associated with the key
     * @throws AutomationException if an error occurs
     */
    public ConfigValue getConfig(String key) throws AutomationException {
        return getConfig(key, false);
    }

    /**
     * Returns the config value associated with the specified key.
     *
     * @param key  the key to use for the config lookup
     * @param path the key contains a path to a property in a map or list to get
     * @return the value associated with the key
     * @throws AutomationException if an error occurs
     */
    public ConfigValue getConfig(String key, boolean path) throws AutomationException {
        return this.workspace.getConfig(this.name, key, path);
    }

    /**
     * Returns the full config map associated with the stack in the Workspace.
     *
     * @return the config map
     * @throws AutomationException if an error occurs
     */
    public Map<String, ConfigValue> getAllConfig() throws AutomationException {
        return this.workspace.getAllConfig(this.name);
    }

    /**
     * Sets the config key-value pair on the Stack in the associated Workspace.
     *
     * @param key   the config key to set
     * @param value the config value to set
     * @throws AutomationException if an error occurs
     */
    public void setConfig(String key, ConfigValue value) throws AutomationException {
        setConfig(key, value, false);
    }

    /**
     * Sets the config key-value pair on the Stack in the associated Workspace.
     *
     * @param key   the config key to set
     * @param value the config value to set
     * @param path  the key contains a path to a property in a map or list to set
     * @throws AutomationException if an error occurs
     */
    public void setConfig(String key, ConfigValue value, boolean path) throws AutomationException {
        this.workspace.setConfig(this.name, key, value, path);
    }

    /**
     * Sets all specified config values on the stack in the associated Workspace.
     *
     * @param configMap the map of config key-value pairs to set
     * @throws AutomationException if an error occurs
     */
    public void setAllConfig(Map<String, ConfigValue> configMap) throws AutomationException {
        setAllConfig(configMap, false);
    }

    /**
     * Sets all specified config values on the stack in the associated Workspace.
     *
     * @param configMap the map of config key-value pairs to set
     * @param path      the keys contain a path to a property in a map or list to
     *                  set
     * @throws AutomationException if an error occurs
     */
    public void setAllConfig(Map<String, ConfigValue> configMap, boolean path) throws AutomationException {
        this.workspace.setAllConfig(this.name, configMap, path);
    }

    /**
     * Removes the specified config key from the Stack in the associated Workspace.
     *
     * @param key the config key to remove
     * @throws AutomationException if an error occurs
     */
    public void removeConfig(String key) throws AutomationException {
        removeConfig(key, false);
    }

    /**
     * Removes the specified config key from the Stack in the associated Workspace.
     *
     * @param key  the config key to remove
     * @param path the key contains a path to a property in a map or list to remove
     * @throws AutomationException if an error occurs
     */
    public void removeConfig(String key, boolean path) throws AutomationException {
        this.workspace.removeConfig(this.name, key, path);
    }

    /**
     * Removes the specified config keys from the Stack in the associated Workspace.
     *
     * @param keys the config keys to remove
     * @throws AutomationException if an error occurs
     */
    public void removeAllConfig(Collection<String> keys) throws AutomationException {
        removeAllConfig(keys, false);
    }

    /**
     * Removes the specified config keys from the Stack in the associated Workspace.
     *
     * @param keys the config keys to remove
     * @param path the keys contain a path to a property in a map or list to remove
     * @throws AutomationException if an error occurs
     */
    public void removeAllConfig(Collection<String> keys, boolean path) throws AutomationException {
        this.workspace.removeAllConfig(this.name, keys, path);
    }

    /**
     * Gets and sets the config map used with the last update.
     *
     * @return the config map used with the last update
     * @throws AutomationException if an error occurs
     */
    public Map<String, ConfigValue> refreshConfig() throws AutomationException {
        return this.workspace.refreshConfig(this.name);
    }

    /**
     * Adds environments to the end of the stack's import list. Imported
     * environments are merged in order per the ESC merge rules. The list of
     * environments behaves as if it were the import list in an anonymous
     * environment.
     *
     * @param environments list of environments to add to the end of the stack's
     *                     import list
     * @throws AutomationException if an error occurs
     */
    public void addEnvironments(Collection<String> environments) throws AutomationException {
        this.workspace.addEnvironments(this.name, environments);
    }

    /**
     * Removes environments from the stack's import list.
     *
     * @param environment the name of the environment to remove from the stack's
     *                    configuration
     * @throws AutomationException if an error occurs
     */
    public void removeEnvironment(String environment) throws AutomationException {
        this.workspace.removeEnvironment(this.name, environment);
    }

    /**
     * Change the secrets provider for a stack.
     *
     * @param newSecretsProvider the new secrets provider
     * @throws AutomationException if an error occurs
     */
    public void changeSecretsProvider(String newSecretsProvider) throws AutomationException {
        changeSecretsProvider(newSecretsProvider, null);
    }

    /**
     * Change the secrets provider for the stack.
     *
     * @param newSecretsProvider the new secrets provider
     * @param options            the options to change the secrets provider
     * @throws AutomationException if an error occurs
     */
    public void changeSecretsProvider(String newSecretsProvider, @Nullable SecretsProviderOptions options)
            throws AutomationException {
        this.workspace.changeSecretsProvider(this.name, newSecretsProvider, options);
    }

    /**
     * Creates or updates the resources in a stack by executing the program in the
     * Workspace.
     * <p>
     * https://www.pulumi.com/docs/reference/cli/pulumi_up/
     *
     * @return the result of the update
     * @throws AutomationException if an error occurs
     */
    public UpResult up() throws AutomationException {
        return up(null);
    }

    /**
     * Creates or updates the resources in a stack by executing the program in the
     * Workspace.
     * <p>
     * https://www.pulumi.com/docs/reference/cli/pulumi_up/
     *
     * @param options options to customize the behavior of the update
     * @return the result of the update
     * @throws AutomationException if an error occurs
     */
    public UpResult up(UpOptions options) throws AutomationException {
        var execKind = ExecKind.Local;
        var program = this.workspace.program();
        var logger = this.workspace.logger();
        var args = new ArrayList<String>();
        args.add("up");
        args.add("--yes");
        args.add("--skip-preview");

        if (options != null) {
            if (options.getProgram() != null) {
                program = options.getProgram();
            }

            if (options.getLogger() != null) {
                logger = options.getLogger();
            }

            if (options.isExpectNoChanges()) {
                args.add("--expect-no-changes");
            }

            if (options.isDiff()) {
                args.add("--diff");
            }

            if (options.getPlan() != null) {
                args.add("--plan");
                args.add(options.getPlan());
            }

            if (options.getReplaces() != null) {
                for (var item : options.getReplaces()) {
                    args.add("--replace");
                    args.add(item);
                }
            }

            if (options.isTargetDependents()) {
                args.add("--target-dependents");
            }

            if (options.isContinueOnError()) {
                args.add("--continue-on-error");
            }

            applyUpdateOptions(options, args);
        }

        InlineLanguageHost inlineHost = null;

        Consumer<String> onStandardOutput = options != null ? options.onStandardOutput() : null;
        Consumer<String> onStandardError = options != null ? options.onStandardError() : null;
        Consumer<EngineEvent> onEvent = options != null ? options.onEvent() : null;

        try {
            if (program != null) {
                execKind = ExecKind.Inline;
                inlineHost = new InlineLanguageHost(program, logger);
                inlineHost.start();
                args.add("--client=127.0.0.1:" + inlineHost.getPort());
            }

            args.add("--exec-kind");
            args.add(execKind);

            CommandResult upResult;
            try {
                upResult = runCommand(args, CommandRunOptions.builder()
                        .onStandardOutput(onStandardOutput)
                        .onStandardError(onStandardError)
                        .onEngineEvent(onEvent)
                        .build());
            } catch (Exception e) {
                // TODO handle inline host exception
                // if (inlineHost != null && inlineHost.TryGetExceptionInfo(out var
                // exceptionInfo)) {
                // exceptionInfo.Throw();
                // }

                // this won't be hit if we have an inline
                // program exception
                throw e;
            }

            var output = getOutputs();
            var showSecrets = options != null && options.isShowSecrets();
            var summary = getInfo(showSecrets);
            return new UpResult(
                    upResult.standardOutput(),
                    upResult.standardError(),
                    summary.get(),
                    output);
        } catch (AutomationException e) {
            throw e;
        } catch (Exception e) {
            throw new AutomationException(e);
        } finally {
            if (inlineHost != null) {
                inlineHost.stop();
            }
        }
    }

    /**
     * Performs a dry-run update to a stack, returning pending changes.
     *
     * @return the result of the preview
     * @throws AutomationException if an error occurs
     */
    public PreviewResult preview() throws AutomationException {
        return preview(null);
    }

    /**
     * Performs a dry-run update to a stack, returning pending changes.
     *
     * @param options options to customize the behavior of the update
     * @return the result of the preview
     * @throws AutomationException if an error occurs
     */
    public PreviewResult preview(PreviewOptions options) throws AutomationException {
        var execKind = ExecKind.Local;
        var program = this.workspace.program();
        var logger = this.workspace.logger();
        var args = new ArrayList<String>();
        args.add("preview");

        if (options != null) {
            if (options.program() != null) {
                program = options.program();
            }

            if (options.logger() != null) {
                logger = options.logger();
            }

            if (options.isExpectNoChanges()) {
                args.add("--expect-no-changes");
            }

            if (options.isDiff()) {
                args.add("--diff");
            }

            if (options.plan() != null) {
                args.add("--plan");
                args.add(options.plan());
            }

            if (options.replaces() != null) {
                for (var item : options.replaces()) {
                    args.add("--replace");
                    args.add(item);
                }
            }

            if (options.targetDependents()) {
                args.add("--target-dependents");
            }

            applyUpdateOptions(options, args);
        }

        InlineLanguageHost inlineHost = null;

        Consumer<String> onStandardOutput = options != null ? options.onStandardOutput() : null;
        Consumer<String> onStandardError = options != null ? options.onStandardError() : null;

        // Get the summary event from the event log.
        // Since Java requires effectively final variables for lambda captures,
        // and we want to update the value, we'll use a single-element array as
        // a workaround, updating the value at index 0.
        SummaryEvent[] summaryEvent = { null };
        Consumer<EngineEvent> onEvent = options != null ? options.onEvent() : null;
        Consumer<EngineEvent> onPreviewEvent = event -> {
            if (event.getSummaryEvent() != null) {
                summaryEvent[0] = event.getSummaryEvent();
            }

            if (onEvent != null) {
                onEvent.accept(event);
            }
        };

        try {
            if (program != null) {
                execKind = ExecKind.Inline;
                inlineHost = new InlineLanguageHost(program, logger);
                inlineHost.start();
                args.add("--client=127.0.0.1:" + inlineHost.getPort());
            }

            args.add("--exec-kind");
            args.add(execKind);

            CommandResult result;
            try {
                result = runCommand(args, CommandRunOptions.builder()
                        .onStandardOutput(onStandardOutput)
                        .onStandardError(onStandardError)
                        .onEngineEvent(onPreviewEvent)
                        .build());
            } catch (Exception e) {
                // TODO handle inline host exception
                // if (inlineHost != null && inlineHost.TryGetExceptionInfo(out var
                // exceptionInfo)) {
                // exceptionInfo.Throw();
                // }

                // this won't be hit if we have an inline
                // program exception
                throw e;
            }

            if (summaryEvent[0] == null) {
                throw new NoSummaryEventException("No summary of changes for 'preview'");
            }

            return new PreviewResult(
                    result.standardOutput(),
                    result.standardError(),
                    summaryEvent[0].getResourceChanges());
        } catch (AutomationException e) {
            throw e;
        } catch (Exception e) {
            throw new AutomationException(e);
        } finally {
            if (inlineHost != null) {
                inlineHost.stop();
            }
        }
    }

    /**
     * Compares the current stack's resource state with the state known to exist in
     * the actual cloud provider. Any such changes are adopted into the current
     * stack.
     *
     * @return the result of the refresh
     * @throws AutomationException if an error occurs
     */
    public UpdateResult refresh() throws AutomationException {
        return refresh(null);
    }

    /**
     * Compares the current stack's resource state with the state known to exist in
     * the actual cloud provider. Any such changes are adopted into the current
     * stack.
     *
     * @param options options to customize the behavior of the refresh
     * @return the result of the refresh
     * @throws AutomationException if an error occurs
     */
    public UpdateResult refresh(RefreshOptions options) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("refresh");
        args.add("--yes");
        args.add("--skip-preview");

        if (options != null) {
            if (options.expectNoChanges()) {
                args.add("--expect-no-changes");
            }

            if (options.skipPendingCreates()) {
                args.add("--skip-pending-creates");
            }

            if (options.clearPendingCreates()) {
                args.add("--clear-pending-creates");
            }

            if (options.importPendingCreates() != null) {
                for (var item : options.importPendingCreates()) {
                    args.add("--import-pending-creates");
                    args.add(item.urn());
                    args.add("--import-pending-creates");
                    args.add(item.id());
                }
            }

            applyUpdateOptions(options, args);
        }

        var execKind = workspace.program() == null ? ExecKind.Local : ExecKind.Inline;
        args.add("--exec-kind");
        args.add(execKind);

        Consumer<String> onStandardOutput = options != null ? options.onStandardOutput() : null;
        Consumer<String> onStandardError = options != null ? options.onStandardError() : null;
        Consumer<EngineEvent> onEvent = options != null ? options.onEvent() : null;

        var result = runCommand(args, CommandRunOptions.builder()
                .onStandardOutput(onStandardOutput)
                .onStandardError(onStandardError)
                .onEngineEvent(onEvent)
                .build());

        var showSecrets = options != null && options.showSecrets();
        var summary = getInfo(showSecrets);
        return new UpdateResult(
                result.standardOutput(),
                result.standardError(),
                summary.get());
    }

    /**
     * Destroy deletes all resources in a stack, leaving all history and
     * configuration intact.
     *
     * @return the result of the destroy
     * @throws AutomationException if an error occurs
     */
    public UpdateResult destroy() throws AutomationException {
        return destroy(null);
    }

    /**
     * Destroy deletes all resources in a stack, leaving all history and
     * configuration intact.
     *
     * @param options options to customize the behavior of the destroy
     * @return the result of the destroy
     * @throws AutomationException if an error occurs
     */
    public UpdateResult destroy(DestroyOptions options) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("destroy");
        args.add("--yes");
        args.add("--skip-preview");

        if (options != null) {
            if (options.isTargetDependents()) {
                args.add("--target-dependents");
            }

            if (options.isContinueOnError()) {
                args.add("--continue-on-error");
            }

            applyUpdateOptions(options, args);
        }

        var execKind = workspace.program() == null ? ExecKind.Local : ExecKind.Inline;
        args.add("--exec-kind");
        args.add(execKind);

        Consumer<String> onStandardOutput = options != null ? options.onStandardOutput() : null;
        Consumer<String> onStandardError = options != null ? options.onStandardError() : null;
        Consumer<EngineEvent> onEvent = options != null ? options.onEvent() : null;

        var result = runCommand(args, CommandRunOptions.builder()
                .onStandardOutput(onStandardOutput)
                .onStandardError(onStandardError)
                .onEngineEvent(onEvent)
                .build());

        var showSecrets = options != null && options.isShowSecrets();
        var summary = getInfo(showSecrets);
        return new UpdateResult(
                result.standardOutput(),
                result.standardError(),
                summary.get());
    }

    /**
     * Gets the current set of Stack outputs from the last {@link WorkspaceStack#up}
     * call.
     *
     * @return the current set of stack outputs
     * @throws AutomationException if an error occurs
     */
    public Map<String, OutputValue> getOutputs() throws AutomationException {
        return this.workspace.getStackOutputs(this.name);
    }

    /**
     * Returns a list summarizing all previews and current results from Stack
     * lifecycle operations (up/preview/refresh/destroy).
     *
     * @return the list of summaries
     * @throws AutomationException if an error occurs
     */
    public List<UpdateSummary> getHistory() throws AutomationException {
        return getHistory(null);
    }

    /**
     * Returns a list summarizing all previews and current results from Stack
     * lifecycle operations (up/preview/refresh/destroy).
     *
     * @param options options to customize the behavior of the fetch history action
     * @return the list of summaries
     * @throws AutomationException if an error occurs
     */
    public List<UpdateSummary> getHistory(HistoryOptions options) throws AutomationException {
        var args = new ArrayList<String>();
        args.add("stack");
        args.add("history");
        args.add("--json");

        if (options != null) {
            if (options.showSecrets()) {
                args.add("--show-secrets");
            }

            var pageSize = options.pageSize();
            if (pageSize != null) {
                if (pageSize < 1) {
                    throw new IllegalArgumentException("Page size must be greater than or equal to 1.");
                }

                var page = options.page();
                page = page == null || page < 1 ? 1 : page;

                args.add("--page-size");
                args.add(String.valueOf(pageSize));
                args.add("--page");
                args.add(String.valueOf(page));
            }
        }

        var result = runCommand(args);
        if (result.standardOutput().isBlank()) {
            return Collections.emptyList();
        }

        var serializer = new LocalSerializer();
        var listType = new TypeToken<List<UpdateSummary>>() {
        }.getType();
        return serializer.deserializeJson(result.standardOutput(), listType);
    }

    /**
     * Exports the deployment state of the stack.
     * <p>
     * This can be combined with {@link #importStack} to edit a
     * stack's state (such as recovery from failed deployments).
     *
     * @return the deployment state of the stack
     * @throws AutomationException if an error occurs
     */
    public StackDeployment exportStack() throws AutomationException {
        return this.workspace.exportStack(this.name);
    }

    /**
     * Imports the specified deployment state into a pre-existing stack.
     * <p>
     * This can be combined with {@link #exportStack} to edit a
     * stack's state (such as recovery from failed deployments).
     *
     * @param state the deployment state to import
     * @throws AutomationException if an error occurs
     */
    public void importStack(StackDeployment state) throws AutomationException {
        this.workspace.importStack(this.name, state);
    }

    /**
     * Returns the current update summary for the stack.
     *
     * @return the current update summary for the stack
     * @throws AutomationException if an error occurs
     */
    public Optional<UpdateSummary> getInfo() throws AutomationException {
        return getInfo(true);
    }

    private Optional<UpdateSummary> getInfo(boolean showSecrets) throws AutomationException {
        var history = getHistory(HistoryOptions.builder()
                .pageSize(1)
                .showSecrets(showSecrets)
                .build());
        return history.stream().findFirst();
    }

    /**
     * Cancel stops a stack's currently running update. It throws
     * an exception if no update is currently running. Note that
     * this operation is _very dangerous_, and may leave the
     * stack in an inconsistent state if a resource operation was
     * pending when the update was canceled. This command is not
     * supported for local backends.
     *
     * @throws AutomationException if an error occurs
     */
    public void cancel() throws AutomationException {
        var args = List.of("cancel", "--stack", this.name, "--yes");
        this.workspace.runCommand(args);
    }

    CommandResult runCommand(List<String> args) throws AutomationException {
        return runCommand(args, CommandRunOptions.EMPTY);
    }

    CommandResult runCommand(List<String> args, CommandRunOptions options) throws AutomationException {
        var newArgs = new ArrayList<String>(args);
        newArgs.add("--stack");
        newArgs.add(this.name);
        return workspace.runCommand(args, options);
    }

    @Override
    public void close() throws Exception {
        workspace.close();
    }

    private static class ExecKind {
        public static final String Local = "auto.local";
        public static final String Inline = "auto.inline";
    }

    private enum WorkspaceStackInitMode {
        CREATE,
        SELECT,
        CREATE_OR_SELECT
    }

    private static class InlineLanguageHost {
        private final Consumer<Context> program;
        private final Logger logger;
        private Server server;

        public InlineLanguageHost(Consumer<Context> program, Logger logger) {
            this.program = program;
            this.logger = logger;
        }

        public void start() throws IOException {
            // maxRpcMessageSize raises the gRPC Max Message size from `4194304` (4mb) to
            // `419430400` (400mb)
            var maxRpcMessageSizeInBytes = 400 * 1024 * 1024;
            server = ServerBuilder.forPort(0)
                    .maxInboundMessageSize(maxRpcMessageSizeInBytes)
                    .addService(new LanguageRuntimeImpl(program, logger))
                    .build()
                    .start();
        }

        public int getPort() {
            return server.getPort();
        }

        public void stop() {
            if (server != null) {
                server.shutdown();
            }
        }
    }

    private static void applyUpdateOptions(UpdateOptions options, ArrayList<String> args) {
        var parallel = options.parallel();
        if (parallel != null) {
            args.add("--parallel");
            args.add(parallel.toString());
        }

        var message = options.Message();
        if (message != null && !message.isBlank()) {
            args.add("--message");
            args.add(message);
        }

        var targets = options.targets();
        if (targets != null) {
            for (var item : targets) {
                args.add("--target");
                args.add(item);
            }
        }

        var policyPacks = options.policyPacks();
        if (policyPacks != null) {
            for (var item : policyPacks) {
                args.add("--policy-pack");
                args.add(item);
            }
        }

        var policyPackConfigs = options.policyPackConfigs();
        if (policyPackConfigs != null) {
            for (var item : policyPackConfigs) {
                args.add("--policy-pack-configs");
                args.add(item);
            }
        }

        var color = options.color();
        if (color != null && !color.isBlank()) {
            args.add("--color");
            args.add(color);
        }

        if (options.logFlow()) {
            args.add("--logflow");
        }

        var verbosity = options.logVerbosity();
        if (verbosity != null) {
            args.add("--verbose");
            args.add(verbosity.toString());
        }

        if (options.logToStdErr()) {
            args.add("--logtostderr");
        }

        var tracing = options.tracing();
        if (tracing != null && !tracing.isBlank()) {
            args.add("--tracing");
            args.add(tracing);
        }

        if (options.debug()) {
            args.add("--debug");
        }

        if (options.json()) {
            args.add("--json");
        }
    }
}
