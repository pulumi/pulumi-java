package com.pulumi.internal;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.resources.StackOptions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.pulumi.resources.Stack.*;
import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class PulumiInternal implements Pulumi {

    protected final Runner runner;
    private final ContextInternal stackContext;

    public PulumiInternal(Runner runner, ContextInternal stackContext) {
        this.runner = requireNonNull(runner);
        this.stackContext = requireNonNull(stackContext);
    }

    @InternalUse
    public static PulumiInternal fromEnvironment() {
        var deployment = DeploymentImpl.fromEnvironment();
        var runner = deployment.getRunner();
        var ctx = contextFromDeployment(deployment);
        return new PulumiInternal(runner, ctx);
    }

    // TODO: remove after refactoring Deployment
    protected static ContextInternal contextFromDeployment(DeploymentImpl deployment) {
        var instance = Deployment.getInstance();
        var projectName = deployment.getProjectName();
        var stackName = deployment.getStackName();
        var runner = deployment.getRunner();
        var log = deployment.getLog();
        Function<String, Config> configFactory = (name) -> new Config(instance.getConfig(), name);
        var config = new ConfigContextInternal(projectName, configFactory);
        var logging = new LoggingContextInternal(log);
        var outputFactory = new OutputFactory(runner);
        var outputs = new OutputContextInternal(outputFactory);
        var exports = Map.<String, Output<?>>of();

        return new ContextInternal(projectName, stackName, logging, config, outputs, exports);
    }

    @InternalUse
    public CompletableFuture<Integer> runAsync(Consumer<Context> stackCallback) {
        final var stackFuture = CompletableFuture.supplyAsync(() -> {
            // before user code was executed
            stackCallback.accept(stackContext); // MUST run before accessing mutable variables
            // after user code was executed
            return this.stackContext.exports();
        });
        runner.registerTask("stackFuture", stackFuture);
        return runner.registerAndRunAsync(
                () -> {
                    var exportsOutput = Output.of(stackFuture);
                    var stack = StackInternal.of(
                            () -> stackFuture,
                            StackOptions.Empty
                    );

                    // set a derived class as the deployment stack
                    DeploymentInternal.getInstance().setStack(stack);
                    DeploymentInternal.getInstance().registerResourceOutputs(stack, exportsOutput);
                }
        );
    }
}
