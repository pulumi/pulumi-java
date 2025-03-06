package com.pulumi.resources.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
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

/**
 * An internal Resource subclass that represent the Pulumi root resource a.k.a. the Stack
 */
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

    private final Output<Map<String, Output<?>>> outputs;

    /**
     * Create and register a Stack.
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

    /**
     * @return the outputs of this {@link Stack}
     */
    @InternalUse
    public Output<Map<String, Output<?>>> outputs() {
        return this.outputs;
    }

    /**
     * A factory of a fully initialized {@link Stack}.
     *
     * @param projectName             the project name associated with this {@link Stack}
     * @param stackName               the stack name associated with this {@link Stack}
     * @param resourceTransformations the {@link Stack} transformations to apply
     * @return a new fully initialized {@link Stack} instance factory
     * that when called will return a new and ready to use instance.
     */
    @InternalUse
    public static Function<Supplier<Map<String, Output<?>>>, Stack> factory(

            String projectName,
            String stackName,
            List<ResourceTransformation> resourceTransformations
    ) {
        /*
         * Stack initialization requires specific sequence of events because of cyclic dependencies.
         * We break the cycles using lazy initialization of the future that backs the Stack outputs Output.
         *
         * Cycles:
         * 1. Stack outputs depend on (get their values from) the user code (callback) that will provide the outputs.
         * 2. User code (callback) will attempt to create new Resource subclasses and that will require Stack instance.
         *
         * Sequence:
         * 1. Create a Stack instance (note that the super constructor calls readOrRegisterResource).
         *    Stack must be the first Resource to be constructed,
         *    before any user code attempts to construct any Resource object.
         *
         * 2. Set the Stack instance in the global state. User code will try to reach it via Resource constructor.
         *    Since Stack is the "universal parent" any time a Resource has no parent the stack will be used.
         *
         * 3. Register the Stack outputs in the Pulumi engine. The Stack itself was registered in
         *    the Resource super-constructor. Registering outputs completes registering a Stack resource.
         *    This operation will complete asynchronously, after the Stack lazy outputs are completed.
         *
         * 4. Call the user code callback (asynchronously to avoid any additional cyclic dependency problems).
         *    At this point the Stack instance must be available from the global state.
         *
         * 5. Complete the Stack initialization (by completing the lazyFuture).
         *    This makes sure we provide a completed future to the Stack outputs.
         */
        var deployment = DeploymentInternal.getInstance(); // TODO: pass as a parameter after refactoring the Deployment initialization
        return (Supplier<Map<String, Output<?>>> callback) -> {
            var lazyFuture = new CompletableFuture<Map<String, Output<?>>>();
            var lazyOutputs = Output.of(lazyFuture);
            var stack = new Stack(projectName, stackName, resourceTransformations, lazyOutputs);
            deployment.setStack(stack);
            deployment.registerResourceOutputs(stack, lazyOutputs);
            // run the user code callback after Stack was set globally, (note that future is eager)
            var callbackFuture = ContextAwareCompletableFuture.supplyAsync(callback);
            deployment.getRunner().registerTask("callback", callbackFuture);
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
