package com.pulumi.internal;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Log;
import com.pulumi.Pulumi;
import com.pulumi.context.internal.ConfigContextInternal;
import com.pulumi.context.internal.ContextInternal;
import com.pulumi.context.internal.LoggingContextInternal;
import com.pulumi.context.internal.OutputContextInternal;
import com.pulumi.core.Output;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputFactory;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.DeploymentImpl;
import com.pulumi.deployment.internal.DeploymentImpl.DeploymentState;
import com.pulumi.deployment.internal.DeploymentInstanceHolder;
import com.pulumi.deployment.internal.DeploymentInstanceInternal;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.deployment.internal.Runner;
import com.pulumi.deployment.internal.Runner.Result;
import com.pulumi.resources.Stack;
import com.pulumi.resources.StackOptions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pulumi.resources.Stack.StackInternal;
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
        var state = DeploymentState.fromEnvironment();
        var deployment = new DeploymentImpl(state);
        var ctx = contextFromDeployment(state.log, deployment);
        return new PulumiInternal(state.runner, ctx);
    }

    // TODO: remove after refactoring Deployment
    protected static ContextInternal contextFromDeployment(Log log, DeploymentInternal deployment) {
        DeploymentInstanceHolder.setInstance(new DeploymentInstanceInternal(deployment));
        var instance = Deployment.getInstance();
        var projectName = deployment.getProjectName();
        var stackName = deployment.getStackName();
        var runner = deployment.getRunner();
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
        return runAsyncResult(userProgram(stackCallback)).thenApply(Result::exitCode);
    }

    @InternalUse
    protected CompletableFuture<Result<Stack>> runAsyncResult(
            Function<ContextInternal, Map<String, Output<?>>> stackCallback
    ) {
        // TODO: is there a way to simplify this nesting doll?
        final Supplier<CompletableFuture<Map<String, Output<?>>>> nestingDoll =
                () -> CompletableFuture.supplyAsync(
                        () -> CompletableFuture.supplyAsync(
                                () -> stackCallback.apply(this.stackContext)
                        )
                ).thenCompose(Function.identity());

        return runAsyncFuture(nestingDoll, StackOptions.Empty);
    }

    protected CompletableFuture<Result<Stack>> runAsyncFuture(
            Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options
    ) {
        /*
          There is an internal circular dependency between any Resource and Stack.
          As a consequence, the user code will need to be run after Stack instance is fully initialized,
          because user code will create new Resources, that require this circular dependency to be already in place.
          Any Resource subclass constructor call before Stack is initialized will fail at runtime.

          To solve this issue the user code will be run after the Stack outputs are registered.
         */
        return this.runner.registerAndRunAsync(() -> {
            // Create and initialize the stack in the context of the Runner's error handler
            var stack = StackInternal.of(callback, options);
            // Make sure the output task is registered
            this.runner.registerTask(
                    String.format("runAsyncFuture: %s, %s", stack.getResourceType(), stack.getResourceName()),
                    Internal.of(stack.outputs()).getDataAsync()
            );
            return stack;
        });
    }

    protected Function<ContextInternal, Map<String, Output<?>>> userProgram(Consumer<Context> stackCallback) {
        return (ctx) -> {
            // before user code was executed
            stackCallback.accept(ctx); // MUST run before accessing mutable variables
            // after user code was executed
            return ctx.exports();
        };
    }
}
