package io.pulumi;

import io.grpc.Internal;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.core.internal.annotations.OutputMetadata;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.exceptions.RunException;
import io.pulumi.resources.ComponentResource;
import io.pulumi.resources.ComponentResourceOptions;
import io.pulumi.resources.Resource;
import io.pulumi.resources.StackOptions;

import javax.annotation.Nullable;
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
     * Constant to represent the 'root stack' resource for a Pulumi application.
     * The purpose of this is solely to make it easy to write an @see {@link io.pulumi.core.Alias} like so:
     * <p>
     * <code>aliases = { new Alias(..., /* parent *&#47; Stack.InternalRoot, ... } }</code>
     * </p>
     * This indicates that the prior name for a resource was created based on it being parented
     * directly by the stack itself and no other resources. Note: this is equivalent to:
     * <p>
     * <code>aliases = { new Alias(..., /* parent *&#47; null, ...) }</code>
     * </p>
     * However, the former form is preferable as it is more self-descriptive, while the latter
     * may look a bit confusing and may incorrectly look like something that could be removed
     * without changing semantics.
     */
    @Internal
    @Nullable
    public static final Resource InternalRoot = null;

    /**
     * The type name that should be used to construct the root component in the tree of Pulumi resources
     * allocated by a deployment. This must be kept up to date with
     * "github.com/pulumi/pulumi/sdk/v3/go/common/resource/stack.RootStackType".
     */
    @Internal
    public static final String InternalRootPulumiStackTypeName = "pulumi:pulumi:Stack";

    /**
     * The outputs of this stack, if the <code>init</code> callback exited normally.
     */
    private Output<Map<String, Optional<Object>>> outputs = Output.of(Map.of());

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
                InternalRootPulumiStackTypeName,
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
    @Internal
    public Stack(Supplier<CompletableFuture<Map<String, Optional<Object>>>> init, @Nullable StackOptions options) {
        this(options);
        try {
            this.outputs = Output.of(runInitAsync(init));
        } finally {
            this.registerOutputs(this.outputs);
        }
    }

    @Internal
    public Output<Map<String, Optional<Object>>> internalGetOutputs() {
        return outputs;
    }

    /**
     * Inspect all public properties of the stack to find outputs.
     * Validate the values and register them as stack outputs.
     */
    @Internal
    public void internalRegisterPropertyOutputs() {
        var infos = OutputMetadata.of(this.getClass());

        var outputs = infos.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getFieldValue(this)
                ));

        var nulls = outputs.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!nulls.isEmpty()) {
            throw new RunException(String.format(
                    "Output(s) '%s' have no value assigned. %s annotated fields must be assigned inside Stack constructor.",
                    String.join(", ", nulls), OutputExport.class.getSimpleName()
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
                    String.join(", ", wrongFields), OutputExport.class.getSimpleName()
            ));
        }


        this.outputs = Output.of(outputs.entrySet().stream().collect(
                toImmutableMap(Map.Entry::getKey, Map.Entry::getValue,
                        (o1, o2) -> {
                            throw new IllegalStateException("Duplicate key");
                        })
        ));
        this.registerOutputs(this.outputs);
    }

    private static CompletableFuture<Map<String, Optional<Object>>> runInitAsync(
            Supplier<CompletableFuture<Map<String, Optional<Object>>>> init
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

}
