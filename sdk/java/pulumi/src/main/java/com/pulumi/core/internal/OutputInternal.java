package com.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import com.pulumi.core.Output;
import com.pulumi.core.Tuples;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.deployment.internal.DeploymentInternal;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public final class OutputInternal<T> implements Output<T>, Copyable<Output<T>> {

    @InternalUse
    public static final Output<Tuples.Tuple0> TupleZeroOut = Output.of(Tuples.Tuple0.Empty);

    private final CompletableFuture<OutputData<T>> dataFuture;

    @InternalUse
    public OutputInternal(@Nullable T value) {
        this(value, false);
    }

    @InternalUse
    public OutputInternal(@Nullable T value, boolean isSecret) {
        this(CompletableFuture.completedFuture(value), isSecret);
    }

    @InternalUse
    public OutputInternal(CompletableFuture<T> value, boolean isSecret) {
        this(OutputData.ofAsync(requireNonNull(value), isSecret));
    }

    @InternalUse
    public OutputInternal(OutputData<T> dataFuture) {
        this(CompletableFuture.completedFuture(requireNonNull(dataFuture)));
    }

    @InternalUse
    public OutputInternal(CompletableFuture<OutputData<T>> dataFuture) {
        this(dataFuture, 0);

        var deployment = DeploymentInternal.getInstanceOptional();
        deployment.ifPresent(deploymentInternal -> deploymentInternal.getRunner().registerTask(
                this.getClass().getTypeName() + " -> " + this.dataFuture, this.dataFuture
        ));
    }

    /**
     * Do not use directly, use {@link OutputFactory}
     */
    @InternalUse
    OutputInternal(CompletableFuture<OutputData<T>> dataFuture, int dummy /* FIXME: remove dummy after refactoring complete */) {
        this.dataFuture = ContextAwareCompletableFuture.wrap(requireNonNull(dataFuture));
    }

    @InternalUse
    public OutputInternal(Set<Resource> resources, T value) {
        this(CompletableFuture.completedFuture(
                OutputData.of(ImmutableSet.copyOf(resources), value)));
    }

    @Override
    public <U> Output<U> apply(Function<T, Output<U>> func) {
        return new OutputInternal<>(OutputData.apply(
                dataFuture,
                func.andThen(o -> Internal.of(o).getDataAsync())
        ));
    }

    public Output<T> copy() {
        // we do not copy the OutputData, because it should be immutable
        return new OutputInternal<>(this.dataFuture.copy()); // TODO: is the copy deep enough
    }

    public Output<T> asPlaintext() { // TODO: this look very unsafe to be exposed, what are the use cases? do we need this?
        return withIsSecret(CompletableFuture.completedFuture(false));
    }

    public Output<T> asSecret() {
        return withIsSecret(CompletableFuture.completedFuture(true));
    }

    @InternalUse
    public Output<T> withIsSecret(CompletableFuture<Boolean> isSecretFuture) {
        return new OutputInternal<>(
                isSecretFuture.thenCompose(
                        secret -> this.dataFuture.thenApply(
                                d -> d.withIsSecret(secret)
                        )
                )
        );
    }

    @InternalUse
    public CompletableFuture<OutputData<T>> getDataAsync() {
        return this.dataFuture;
    }

    @InternalUse
    public CompletableFuture</* @Nullable */T> getValueOrDefault(@Nullable T defaultValue) {
        return this.dataFuture.thenApply(d -> d.getValueOrDefault(defaultValue));
    }

    @InternalUse
    public CompletableFuture</* @Nullable */ T> getValueNullable() {
        return this.dataFuture.thenApply(OutputData::getValueNullable);
    }

    @InternalUse
    public CompletableFuture<Optional<T>> getValueOptional() {
        return this.dataFuture.thenApply(OutputData::getValueOptional);
    }

    @InternalUse
    public CompletableFuture<ImmutableSet<Resource>> getResources() {
        return this.dataFuture.thenApply(OutputData::getResources);
    }

    @InternalUse
    public CompletableFuture<Boolean> isKnown() {
        return this.dataFuture.thenApply(OutputData::isKnown);
    }

    @InternalUse
    public CompletableFuture<Boolean> isSecret() {
        return this.dataFuture.thenApply(OutputData::isSecret);
    }

    @Override
    public String toString() {
        var message = String.join(System.lineSeparator(),
                "Calling 'toString' on an 'Output<T>' is not supported.",
                "This is because the value of an 'Output' is asynchronous.",
                "",
                "To get the value of an Output<T> as an Output<String> consider either:",
                "1: applyValue(v -> String.format(\"prefix%ssuffix\", v))",
                "2: Output.format(\"prefix%ssuffix\", o)",
                "",
                "See https://www.pulumi.com/docs/concepts/inputs-outputs for more details.");

        var errorOutputString = System.getenv("PULUMI_ERROR_OUTPUT_STRING");

        if ("1".equals(errorOutputString) || "true".equalsIgnoreCase(errorOutputString)) {
            throw new IllegalStateException(message);
        }

        return String.join(System.lineSeparator(), message,
                "This function may throw in a future version of Pulumi.");
    }

    // Static section -----

    static <T> OutputInternal<T> cast(Output<T> output) {
        requireNonNull(output);
        if (output instanceof OutputInternal) {
            return (OutputInternal<T>) output;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Expected a 'OutputInternal<T>' instance, got: %s",
                    output.getClass().getSimpleName()
            ));
        }
    }
}
