package io.pulumi;

import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;
import io.pulumi.core.internal.Internal.InternalField;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.core.internal.annotations.OutputMetadata;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.ComponentResource;
import io.pulumi.resources.ComponentResourceOptions;
import io.pulumi.resources.StackOptions;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public class Stack extends ComponentResource {

    /**
     * The outputs of this stack, if the <code>init</code> callback exited normally.
     */
    private Output<Map<String, Output<?>>> outputs = Output.of(Map.of());

    @SuppressWarnings("unused")
    @InternalField
    private final StackInternal internal = new StackInternal(this);

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
                StackInternal.RootPulumiStackTypeName,
                String.format("%s-%s", Deployment.getInstance().getProjectName(), Deployment.getInstance().getStackName()),
                convertOptions(options)
        );
        // set a derived class as the deployment stack
        DeploymentInternal.getInstance().setStack(this);
    }

    /**
     * Create a Stack with stack resources created by the <code>init</code> callback.
     * An instance of this will be automatically created when
     * any @see {@link Deployment#runAsync(Supplier)} overload is called.
     */
    @InternalUse
    private Stack(Supplier<CompletableFuture<Map<String, Output<?>>>> init, @Nullable StackOptions options) {
        this(options);
        try {
            this.outputs = Output.of(runInitAsync(init));
        } finally {
            this.registerOutputs(this.outputs);
        }
    }

    private static CompletableFuture<Map<String, Output<?>>> runInitAsync(
            Supplier<CompletableFuture<Map<String, Output<?>>>> init
    ) {
        return CompletableFuture.supplyAsync(init).thenCompose(Function.identity());
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
    public final static class StackInternal extends ComponentResourceInternal {

        private final Stack stack;

        private StackInternal(Stack stack) {
            super(stack);
            this.stack = requireNonNull(stack);
        }

        @InternalUse
        public Output<Map<String, Output<?>>> getOutputs() {
            return this.stack.outputs;
        }

        /**
         * Inspect all public properties of the stack to find outputs.
         * Validate the values and register them as stack outputs.
         */
        @InternalUse
        public void registerPropertyOutputs() {
            var infos = OutputMetadata.of(this.stack.getClass()); // we need the outer class

            var outputs = infos.entrySet().stream()
                    .collect(toImmutableMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            var nulls = outputs.entrySet().stream()
                    .filter(entry -> entry.getValue().isFieldNull(this.stack))
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


            this.stack.outputs = Output.of(
                    outputs.entrySet().stream()
                            .collect(toImmutableMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().getFieldValueOrThrow(this.stack, () -> new IllegalStateException(
                                            "Expected only non-null values at this point. This is a bug."
                                    ))
                            ))
            );
            this.stack.registerOutputs(this.stack.outputs);
        }

        /**
         * The type name that should be used to construct the root component in the tree of Pulumi resources
         * allocated by a deployment. This must be kept up to date with
         * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
         */
        @InternalUse
        public static final String RootPulumiStackTypeName = "pulumi:pulumi:Stack";

        @InternalUse
        public static Stack of(Supplier<CompletableFuture<Map<String, Output<?>>>> callback, StackOptions options) {
            return new Stack(callback, options);
        }
    }
}
