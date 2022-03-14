package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@InternalUse
public abstract class InputOutputInternal<T>
        implements Copyable<Output<T>> {

    @InternalUse
    public static final Output<Tuples.Tuple0> TupleZeroOut = Output.of(Tuples.Tuple0.Empty);

    protected final CompletableFuture<OutputData<T>> dataFuture;

    protected InputOutputInternal(T value) {
        this(value, false);
    }

    protected InputOutputInternal(T value, boolean isSecret) {
        this(CompletableFuture.completedFuture(Objects.requireNonNull(value)), isSecret);
    }

    protected InputOutputInternal(CompletableFuture<T> value, boolean isSecret) {
        this(OutputData.ofAsync(Objects.requireNonNull(value), isSecret));
    }

    protected InputOutputInternal(OutputData<T> dataFuture) {
        this(CompletableFuture.completedFuture(Objects.requireNonNull(dataFuture)));
    }

    protected InputOutputInternal(CompletableFuture<OutputData<T>> dataFuture) {
        this.dataFuture = Objects.requireNonNull(dataFuture);

        var deployment = DeploymentInternal.getInstanceOptional();
        deployment.ifPresent(deploymentInternal -> deploymentInternal.getRunner().registerTask(
                this.getClass().getTypeName() + " -> " + dataFuture, dataFuture
        ));
    }

    protected abstract Output<T> newInstance(CompletableFuture<OutputData<T>> dataFuture);

    public Output<T> copy() {
        // we do not copy the OutputData, because it should be immutable
        return newInstance(this.dataFuture.copy()); // TODO: is the copy deep enough
    }

    public Output<T> asPlaintext() { // TODO: this look very unsafe to be exposed, what are the use cases? do we need this?
        return withIsSecret(CompletableFuture.completedFuture(false));
    }

    public Output<T> asSecret() {
        return withIsSecret(CompletableFuture.completedFuture(true));
    }

    @InternalUse
    public Output<T> withIsSecret(CompletableFuture<Boolean> isSecretFuture) {
        return newInstance(
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

    @InternalUse
    public CompletableFuture<Boolean> isEmpty() {
        return this.dataFuture.thenApply(OutputData::isEmpty);
    }

    @InternalUse
    public CompletableFuture<Boolean> isPresent() {
        return this.dataFuture.thenApply(OutputData::isPresent);
    }

    static <T> InputOutputInternal<T> cast(
            Output<T> output
    ) {
        Objects.requireNonNull(output);
        if (output instanceof InputOutputInternal) {
            //noinspection unchecked
            return (InputOutputInternal<T>) output;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Expected a 'InputOutputInternal<T>' instance, got: %s",
                    output.getClass().getSimpleName()
            ));
        }
    }
}
