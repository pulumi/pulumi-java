package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.Input;
import io.pulumi.core.InputOutput;
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
public abstract class InputOutputInternal<T, IO extends InputOutput<T, IO> & Copyable<IO>>
        implements InputOutput<T, IO> {

    @InternalUse
    public static final Input<Tuples.Tuple0> TupleZeroIn = Input.of(Tuples.Tuple0.Empty);
    @InternalUse
    public static final Output<Tuples.Tuple0> TupleZeroOut = Output.of(Tuples.Tuple0.Empty);

    protected final CompletableFuture<InputOutputData<T>> dataFuture;

    protected InputOutputInternal(T value) {
        this(value, false);
    }

    protected InputOutputInternal(T value, boolean isSecret) {
        this(CompletableFuture.completedFuture(Objects.requireNonNull(value)), isSecret);
    }

    protected InputOutputInternal(CompletableFuture<T> value, boolean isSecret) {
        this(InputOutputData.ofAsync(Objects.requireNonNull(value), isSecret));
    }

    protected InputOutputInternal(InputOutputData<T> dataFuture) {
        this(CompletableFuture.completedFuture(Objects.requireNonNull(dataFuture)));
    }

    protected InputOutputInternal(CompletableFuture<InputOutputData<T>> dataFuture) {
        this.dataFuture = Objects.requireNonNull(dataFuture);

        var deployment = DeploymentInternal.getInstanceOptional();
        deployment.ifPresent(deploymentInternal -> deploymentInternal.getRunner().registerTask(
                this.getClass().getTypeName() + " -> " + dataFuture, dataFuture
        ));
    }

    protected abstract IO newInstance(CompletableFuture<InputOutputData<T>> dataFuture);

    public IO copy() {
        // we do not copy the OutputData, because it should be immutable
        return newInstance(this.dataFuture.copy()); // TODO: is the copy deep enough
    }

    public IO asPlaintext() { // TODO: this look very unsafe to be exposed, what are the use cases? do we need this?
        return withIsSecret(CompletableFuture.completedFuture(false));
    }

    public IO asSecret() {
        return withIsSecret(CompletableFuture.completedFuture(true));
    }

    @InternalUse
    public IO withIsSecret(CompletableFuture<Boolean> isSecretFuture) {
        return newInstance(
                isSecretFuture.thenCompose(
                        secret -> this.dataFuture.thenApply(
                                d -> d.withIsSecret(secret)
                        )
                )
        );
    }

    @InternalUse
    public CompletableFuture<InputOutputData<T>> getDataAsync() {
        return this.dataFuture;
    }

    @InternalUse
    public CompletableFuture</* @Nullable */T> getValueOrDefault(@Nullable T defaultValue) {
        return this.dataFuture.thenApply(d -> d.getValueOrDefault(defaultValue));
    }

    @InternalUse
    public CompletableFuture</* @Nullable */ T> getValueNullable() {
        return this.dataFuture.thenApply(InputOutputData::getValueNullable);
    }

    @InternalUse
    public CompletableFuture<Optional<T>> getValueOptional() {
        return this.dataFuture.thenApply(InputOutputData::getValueOptional);
    }

    @InternalUse
    public CompletableFuture<ImmutableSet<Resource>> getResources() {
        return this.dataFuture.thenApply(InputOutputData::getResources);
    }

    @InternalUse
    public CompletableFuture<Boolean> isKnown() {
        return this.dataFuture.thenApply(InputOutputData::isKnown);
    }

    @InternalUse
    public CompletableFuture<Boolean> isSecret() {
        return this.dataFuture.thenApply(InputOutputData::isSecret);
    }

    @InternalUse
    public CompletableFuture<Boolean> isEmpty() {
        return this.dataFuture.thenApply(InputOutputData::isEmpty);
    }

    @InternalUse
    public CompletableFuture<Boolean> isPresent() {
        return this.dataFuture.thenApply(InputOutputData::isPresent);
    }

    static <T, IO extends InputOutput<T, IO> & Copyable<IO>> InputOutputInternal<T, IO> cast(
            InputOutput<T, IO> inputOutput
    ) {
        Objects.requireNonNull(inputOutput);
        if (inputOutput instanceof InputOutputInternal) {
            return (InputOutputInternal<T, IO>) inputOutput;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Expected a 'InputOutputInternal<T, IO>' instance, got: %s",
                    inputOutput.getClass().getSimpleName()
            ));
        }
    }
}
