package com.pulumi.resources.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.ResourceTransformation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public final class Stack extends ComponentResource {

    /**
     * The type name that should be used to construct the root component in the tree of Pulumi resources
     * allocated by a deployment. This must be kept up to date with
     * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
     */
    @InternalUse
    public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

    /**
     * The outputs of this stack, if the <code>callback</code> callback exited normally.
     */
    private final Output<Map<String, Output<?>>> outputs;

    /**
     * Create a Stack with stack outputs defined in derived class constructor.
     *
     * @param projectName             the current project name
     * @param stackName               the current stack name
     * @param resourceTransformations optional list of transformations to apply to this stack's resources during construction.
     *                                The transformations are applied in order, and are applied after all the transformations of custom
     *                                and component resources in the stack.
     * @param outputs                 the stack outputs
     */
    private Stack(
            String projectName,
            String stackName,
            List<ResourceTransformation> resourceTransformations,
            Output<Map<String, Output<?>>> outputs
    ) {
        super(
                RootPulumiStackTypeName,
                rootPulumiStackName(projectName, stackName),
                ComponentResourceOptions.builder().resourceTransformations(
                        resourceTransformations
                ).build()
        );
        this.outputs = requireNonNull(outputs);
    }

    private static String rootPulumiStackName(String projectName, String stackName) {
        return String.format("%s-%s", requireNonNull(projectName), requireNonNull(stackName));
    }

    @InternalUse
    public Output<Map<String, Output<?>>> getOutputs() {
        return this.outputs;
    }

    @InternalUse
    public static Function<Supplier<Map<String, Output<?>>>, Stack> factory(
            String projectName,
            String stackName,
            List<ResourceTransformation> resourceTransformations
    ) {
        // Stack initialization requires specific sequence of events:
        // - create a Stack instance (note that the super constructor calls readOrRegisterResource)
        // - set the Stack instance in the global state (user code will try to reach it via Resource constructor)
        // - register the Stack outputs in the Pulumi engine
        // - call the user code callback (asynchronously to avoid any additional cyclic dependency problems)
        // - complete the Stack initialization (by completing the lazyFuture)
        return (Supplier<Map<String, Output<?>>> callback) -> {
            // incomplete future that will be completed after the Stack is constructed
            var lazyFuture = new CompletableFuture<Map<String, Output<?>>>();
            // incomplete outputs that will be completed when lazyFuture completes
            var lazyOutputs = Output.of(lazyFuture);
            // Stack must be the first Resource to be constructed,
            // before any user code attempts to construct any Resource
            var stack = new Stack(projectName, stackName, resourceTransformations, lazyOutputs);
            // register the new Stack with the engine
            DeploymentInternal.getInstance().setStack(stack);
            // register the Stack outputs with the engine
            DeploymentInternal.getInstance().registerResourceOutputs(stack, lazyOutputs);
            // run the user code callback wrapped in a future (note that future is eager)
            var callbackFuture = CompletableFuture.supplyAsync(callback);
            DeploymentInternal.getInstance().getRunner().registerTask("callback", callbackFuture);
            // complete the lazyFuture (and lazyOutputs) when user callback future completes
            callbackFuture.whenComplete((value, throwable) -> {
                if (throwable != null) {
                    lazyFuture.completeExceptionally(throwable);
                } else {
                    lazyFuture.complete(value);
                }
            });
            return stack;
        };
    }
}
