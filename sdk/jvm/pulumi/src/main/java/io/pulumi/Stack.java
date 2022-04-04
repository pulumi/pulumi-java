package io.pulumi;

import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;
import io.pulumi.core.internal.Internal.Field;
import io.pulumi.core.internal.OutputBuilder;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.core.internal.annotations.OutputMetadata;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.CurrentDeployment;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.ComponentResource;
import io.pulumi.resources.ComponentResourceOptions;
import io.pulumi.resources.StackOptions;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

public class Stack extends ComponentResource {

    /**
     * The outputs of this stack, if the <code>init</code> callback exited normally.
     */
    private Output<Map<String, Optional<Object>>> outputs;

    @SuppressWarnings("unused")
    @Field
    private final Internal internal = new Internal();

    /**
     * Create a Stack with stack resources defined in derived class constructor.
     * Also @see {@link #Stack(StackOptions)}
     */
    public Stack() {
        this(null);
    }

    /**
     * Create a Stack with stack resources defined in derived class constructor.
     *
     * @param options optional stack options
     */
    public Stack(@Nullable StackOptions options) {
        super(
                InternalStatic.RootPulumiStackTypeName,
                buildStackName(),
                convertOptions(options)
        );
        // set a derived class as the deployment stack
        DeploymentInternal.cast(deployment).setStack(this);
        this.outputs = OutputBuilder.forDeployment(deployment).of(Map.of());
    }

    private static String buildStackName() {
        var deployment = CurrentDeployment.getCurrentDeploymentOrThrow();
        return String.format("%s-%s", deployment.getProjectName(), deployment.getStackName());
    }

    /**
     * Create a Stack with stack resources created by the <code>init</code> callback.
     * An instance of this will be automatically created when
     * any @see {@link Deployment#runAsync(Supplier)} overload is called.
     */
    @InternalUse
    private Stack(Supplier<CompletableFuture<Map<String, Optional<Object>>>> init,
                  @Nullable StackOptions options) {
        this(options);
        try {
            this.outputs = OutputBuilder.forDeployment(deployment).of(runInitAsync(deployment, init));
        } finally {
            this.registerOutputs(this.outputs);
        }
    }

    private static CompletableFuture<Map<String, Optional<Object>>> runInitAsync(
            Deployment deployment,
            Supplier<CompletableFuture<Map<String, Optional<Object>>>> init
    ) {
        return CompletableFuture.supplyAsync(() ->
                        CurrentDeployment.withCurrentDeployment(deployment, () -> init.get()))
                .thenCompose(Function.identity());
    }

    @Nullable
    private static ComponentResourceOptions convertOptions(@Nullable StackOptions options) {
        if (options == null) {
            return null;
        }

        return new ComponentResourceOptions(
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                options.getResourceTransformations(),
                null,
                null,
                null,
                null
        );
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public final class Internal {

        private Internal() {
            /* Empty */
        }

        @InternalUse
        public Output<Map<String, Optional<Object>>> getOutputs() {
            return Stack.this.outputs;
        }

        /**
         * Inspect all public properties of the stack to find outputs.
         * Validate the values and register them as stack outputs.
         */
        @InternalUse
        public void registerPropertyOutputs() {
            var infos = OutputMetadata.of(Stack.this.getClass()); // we need the subclass

            var outputs = infos.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getFieldValue(Stack.this)
                    ));

            var nulls = outputs.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (!nulls.isEmpty()) {
                throw new RunException(String.format(
                        "Output(s) '%s' have no value assigned. %s annotated fields must be assigned inside Stack constructor.",
                        String.join(", ", nulls), Export.class.getSimpleName()
                ));
            }

            // check if annotated fields have the correct type;
            // it would be easier to validate on construction,
            // but we aggregate all errors here for user's convenience
            var wrongFields = infos.entrySet().stream()
                    // check if the field has type allowed by the annotation
                    .filter(entry -> !Output.class.isAssignableFrom(entry.getValue().getFieldType()))
                    .map(Map.Entry::getKey)
                    .collect(toImmutableList());

            if (!wrongFields.isEmpty()) {
                throw new RunException(String.format(
                        "Output(s) '%s' have incorrect type. %s annotated fields must be instances of Output<T>",
                        String.join(", ", wrongFields), Export.class.getSimpleName()
                ));
            }

            var out = OutputBuilder.forDeployment(this.stack.deployment);

            Stack.this.outputs = out.of(
                    outputs.entrySet().stream()
                            .collect(toImmutableMap(Map.Entry::getKey, e -> e.getValue().map(o -> o)))
            );
            Stack.this.registerOutputs(Stack.this.outputs);
        }
    }

    @InternalUse
    public static final class InternalStatic {

        private InternalStatic() {
            throw new UnsupportedOperationException("static class");
        }

        /**
         * The type name that should be used to construct the root component in the tree of Pulumi resources
         * allocated by a deployment. This must be kept up to date with
         * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
         */
        @InternalUse
        public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

        @InternalUse
        public static Stack of(Supplier<CompletableFuture<Map<String, Optional<Object>>>> callback, StackOptions options) {
            return new Stack(callback, options);
        }
    }
}
