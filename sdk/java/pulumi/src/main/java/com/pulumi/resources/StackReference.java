package com.pulumi.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.Maps;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.exceptions.RunException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages a reference to a Pulumi stack and provides access to the referenced stack's outputs.
 */
public class StackReference extends CustomResource {

    /**
     * The name of the referenced stack.
     */
    @Export(name = "name", type = String.class)
    private Output<String> name;

    /**
     * The outputs of the referenced stack.
     */
    @Export(name = "outputs", type = Map.class, parameters = {String.class, Object.class})
    public Output<Map<String, Object>> outputs;

    /**
     * The names of any stack outputs which contain secrets.
     */
    @Export(name = "secretOutputNames", type = List.class, parameters = String.class)
    public Output<List<String>> secretOutputNames;

    /**
     * Create a {@link StackReference} resource with the given unique name.
     * <p>
     * The name of the referenced stack will be the name of the StackReference resource.
     * <br>
     *
     * @param name The unique name of the stack reference.
     * @see StackReference#StackReference(String, StackReferenceArgs, CustomResourceOptions)
     */
    public StackReference(String name) {
        this(name, StackReferenceArgs.Empty, CustomResourceOptions.Empty);
    }

    /**
     * Create a {@link StackReference} resource with the given unique name, arguments.
     * <p>
     * If args is not specified, the name of the referenced stack will be the name of the StackReference resource.
     * <br>
     *
     * @param name The unique name of the stack reference.
     * @param args The arguments to use to populate this resource's properties.
     * @see StackReference#StackReference(String, StackReferenceArgs, CustomResourceOptions)
     */
    public StackReference(String name, @Nullable StackReferenceArgs args) {
        this(name, args, CustomResourceOptions.Empty);
    }

    /**
     * Create a {@link StackReference} resource with the given unique name, arguments, and options.
     * <p>
     * If args is not specified, the name of the referenced stack will be the name of the StackReference resource.
     * <br>
     *
     * @param name    The unique name of the stack reference.
     * @param args    The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public StackReference(String name, @Nullable StackReferenceArgs args, @Nullable CustomResourceOptions options) {
        super(
                "pulumi:pulumi:StackReference",
                name,
                new StackReferenceArgs(ensureName(args, name)),
                CustomResourceOptions.merge(options, CustomResourceOptions.builder().id(ensureName(args, name)).build())
        );
    }

    private static Output<String> ensureName(@Nullable StackReferenceArgs args, String name) {
        return args == null ? Output.of(name) : args.name().orElse(Output.of(name));
    }

    public Output<String> getName() {
        return name;
    }

    public Output<Map<String, Object>> getOutputs() {
        return outputs.applyValue(ImmutableMap::copyOf);
    }

    public Output<List<String>> getSecretOutputNames() {
        return secretOutputNames.applyValue(ImmutableList::copyOf);
    }

    /**
     * Fetches the value of the named stack output, or {@code null} if the stack output was not found.
     * <p>
     *
     * @param name The name of the stack output to fetch.
     * @return An {@link Output} containing the requested value.
     */
    public Output<?> getOutput(String name) {
        return this.getOutput(Output.of(name));
    }

    /**
     * Fetches the value of the named stack output, or {@code null} if the stack output was not found.
     * <p>
     *
     * @param name The name of the stack output to fetch.
     * @return An {@link Output} containing the requested value.
     */
    public Output<?> getOutput(Output<String> name) {
        // Note that this is subtly different from "apply" here. A default "apply" will set the secret bit if any
        // of the inputs are a secret, and this.outputs is always a secret if it contains any secrets.
        // We do this dance, so we can ensure that the Output we return is not needlessly tainted as a secret.
        var value = Output.tuple(name, this.outputs).applyValue(
                v -> Maps.tryGetValue(v.t2, v.t1).orElse(null));

        return Internal.of(value).withIsSecret(isSecretOutputName(name));
    }

    /**
     * Fetches the value of the named stack output, or throws an error if the output was not found.
     * <p>
     *
     * @param name The name of the stack output to fetch.
     * @return An {@link Output} containing the requested value.
     */
    public Output<?> requireOutput(String name) {
        return this.requireOutput(Output.of(name));
    }

    /**
     * Fetches the value of the named stack output, or throws an error if the output was not found.
     * <p>
     *
     * @param name The name of the stack output to fetch.
     * @return An {@link Output} containing the requested value.
     */
    public Output<?> requireOutput(Output<String> name) {
        var value = Output.tuple(name, this.name, this.outputs).applyValue(
                v -> Maps.tryGetValue(v.t3, v.t1).orElseThrow(
                        () -> new KeyMissingException(v.t1, v.t2)));

        return Internal.of(value).withIsSecret(isSecretOutputName(name));
    }

    /**
     * Fetches the value of the named stack output. May return null if the value is
     * not known for some reason.
     * <p>
     * This operation is not supported (and will throw) for secret outputs.
     * </p>
     *
     * @param name The name of the stack output to fetch.
     * @return The value of the referenced stack output.
     */
    public CompletableFuture<?> getValueAsync(String name) {
        return this.getValueAsync(Output.of(name));
    }

    /**
     * Fetches the value of the named stack output. May return null if the value is
     * not known for some reason.
     * <p>
     * This operation is not supported (and will throw) for secret outputs.
     * </p>
     *
     * @param name The name of the stack output to fetch.
     * @return The value of the referenced stack output.
     */
    public CompletableFuture<?> getValueAsync(Output<String> name) {
        return Internal.of(this.getOutput(name)).getDataAsync()
                .thenApply(data -> {
                    if (data.isSecret()) {
                        throw new UnsupportedOperationException(
                                "Cannot call 'getValueAsync' if the referenced stack has secret outputs. Use 'getOutput' instead.");
                    }
                    return data.getValueNullable();
                });
    }

    /**
     * Fetches the value promptly of the named stack output. Throws an error if the stack output is not found.
     * <p>
     * This operation is not supported (and will throw) for secret outputs.
     * </p>
     *
     * @param name The name of the stack output to fetch.
     * @return The value of the referenced stack output.
     */
    public CompletableFuture<?> requireValueAsync(String name) {
        return this.requireValueAsync(Output.of(name));
    }

    /**
     * Fetches the value promptly of the named stack output. Throws an error if the stack output is not found.
     * <p>
     * This operation is not supported (and will throw) for secret outputs.
     * </p>
     *
     * @param name The name of the stack output to fetch.
     * @return The value of the referenced stack output.
     */
    public CompletableFuture<?> requireValueAsync(Output<String> name) {
        return Internal.of(this.requireOutput(name)).getDataAsync()
                .thenApply(data -> {
                    if (data.isSecret()) {
                        throw new UnsupportedOperationException(
                                "Cannot call 'requireValueAsync' if the referenced stack has secret outputs. Use 'requireOutput' instead.");
                    }
                    return data.getValueNullable();
                });
    }

    private CompletableFuture<Boolean> isSecretOutputName(Output<String> name) {
        return Internal.of(name).getDataAsync().thenCompose(
                (OutputData<String> nameOutput) -> Internal.of(this.secretOutputNames).getDataAsync().thenCompose(
                        (OutputData<List<String>> secretOutputNamesData) -> {
                            // If either the name or set of secret outputs is unknown, we can't do anything smart,
                            // so we just copy the secret-ness from the entire outputs value.
                            if (!(nameOutput.isKnown() && secretOutputNamesData.isKnown())) {
                                return Internal.of(this.outputs).isSecret();
                            }

                            // Otherwise, if we have a list of outputs we know are secret, we can use that list to determine if this
                            // output should be secret.
                            var names = secretOutputNamesData.getValueOptional();
                            return CompletableFuture.completedFuture(
                                    names.isPresent() && names.get().contains(nameOutput.getValueNullable())
                            );
                        }
                )
        );
    }

    /**
     * ConfigMissingException is used when a configuration value is completely missing.
     */
    @ParametersAreNonnullByDefault
    @InternalUse
    @VisibleForTesting
    public static class KeyMissingException extends RunException {
        public KeyMissingException(String key, String stack) {
            super(String.format("Required output '%s' does not exist on stack '%s'.", key, stack));
        }
    }
}
