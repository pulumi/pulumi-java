package com.pulumi.automation.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.automation.LocalWorkspace;
import com.pulumi.automation.LocalWorkspaceOptions;
import com.pulumi.automation.ProjectSettings;
import com.pulumi.automation.PulumiAuto;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.DeploymentInstanceInternal;
import com.pulumi.deployment.internal.GrpcEngine;
import com.pulumi.deployment.internal.GrpcMonitor;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.internal.PulumiInternal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Provides an internal Pulumi Automation entrypoint and exposes various internals for the testing purposes.
 */
@InternalUse
@ParametersAreNonnullByDefault
public class PulumiAutoInternal extends PulumiInternal implements PulumiAuto, Closeable {

    public PulumiAutoInternal(
            Runner runner,
            ContextInternal stackContext
    ) {
        super(runner, stackContext);
    }

    public static PulumiAutoInternal from(Logger logger, RunRequestContext requestContext) {
        var engine = new GrpcEngine(requestContext.engineAddress());
        var monitor = new GrpcMonitor(requestContext.monitorAddress());

        var conf = new DeploymentImpl.Config(
                requestContext.configMap(),
                requestContext.configSecretKeys()
        );

        var projectName = requestContext.project();
        var stackName = requestContext.stack();
        var dryRun = requestContext.dryRun();

        var state = new DeploymentImpl.DeploymentState(conf, logger, projectName, stackName, dryRun, engine, monitor);
        var deployment = new DeploymentImpl(state);
        DeploymentInstanceHolder.setInstance(new DeploymentInstanceInternal(deployment));

        var instance = Deployment.getInstance();
        var runner = deployment.getRunner();
        var log = deployment.getLog();

        Function<String, Config> configFactory = (name) -> new Config(instance.getConfig(), name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);
        var ctx = new ContextInternal(
                projectName, stackName, logging, config, outputs, List.of()
        );
        return new PulumiAutoInternal(runner, ctx);
    }

    @InternalUse
    public CompletableFuture<AutoResult> runAutoAsync(
            Consumer<Context> stackCallback
    ) {
        return runAsyncResult(stackCallback).thenApply(r -> new AutoResult(
                r.exitCode(),
                r.exceptions(),
                this.stackContext.exports()
        ));
    }

    @Override
    public void close() {
        // Unset the global state
        DeploymentImpl.internalUnsafeDestroyInstance();
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class APIInternal implements PulumiAuto.API {

        private final Logger standardLogger = Logger.getLogger(PulumiAuto.API.class.getName());

        private ProjectSettings projectSettings;
        private ImmutableMap<String, String> environmentVariables;

        @Override
        public PulumiAuto.API withProjectSettings(ProjectSettings projectSettings) {
            this.projectSettings = requireNonNull(projectSettings);
            return this;
        }

        @Override
        public PulumiAuto.API withEnvironmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = ImmutableMap.copyOf(environmentVariables);
            return this;
        }

        @Override
        public LocalWorkspace localWorkspace(LocalWorkspaceOptions options) {
            return new LocalWorkspace(
                    this.standardLogger,
                    this.projectSettings,
                    this.environmentVariables,
                    options
            );
        }
    }
}
