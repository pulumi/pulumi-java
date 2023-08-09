package com.pulumi.automation.internal;

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
import com.pulumi.deployment.internal.Runner;
import com.pulumi.internal.PulumiInternal;
import com.pulumi.test.internal.PulumiTestInternal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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

        private final PulumiAutoInternal.Builder builder = new PulumiAutoInternal.Builder();

        @Override
        public PulumiAuto.API withProjectSettings(ProjectSettings projectSettings) {
            builder.projectSettings(projectSettings);
            return this;
        }

        @Override
        public PulumiAuto.API withEnvironmentVariables(Map<String, String> environmentVariables) {

            return this;
        }

        @Override
        public PulumiAuto.API withInlineProgram(Consumer<Context> program) {

            return this;
        }

        @Override
        public LocalWorkspace localWorkspace(LocalWorkspaceOptions options) {
            return new LocalWorkspace();
        }
    }

    /**
     * @return a new {@link PulumiAutoInternal.Builder} for {@link PulumiAutoInternal}
     */
    @InternalUse
    public static PulumiAutoInternal.Builder builder() {
        return new PulumiAutoInternal.Builder();
    }

    /**
     * The {@link PulumiAutoInternal} builder.
     */
    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class Builder {

        /**
         * @return a {@link PulumiTestInternal} instance created from this {@link PulumiTestInternal.Builder}
         */
        public PulumiAutoInternal build() {
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
    }
}
