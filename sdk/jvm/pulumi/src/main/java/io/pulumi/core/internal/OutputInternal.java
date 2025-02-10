package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.internal.CurrentDeployment;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@InternalUse
@ParametersAreNonnullByDefault
public final class OutputInternal<T> implements Output<T>, Copyable<Output<T>> {

    private final CompletableFuture<OutputData<T>> dataFuture;

    private final Deployment deployment;

    @InternalUse
    public OutputInternal(Deployment deployment, @Nullable T value) {
        this(deployment, value, false);
    }

    @InternalUse
    public OutputInternal(Deployment deployment, @Nullable T value, boolean isSecret) {
        this(deployment, CompletableFuture.completedFuture(value), isSecret);
    }

    @InternalUse
    public OutputInternal(Deployment deployment, CompletableFuture<T> value, boolean isSecret) {
        this(deployment, OutputData.ofAsync(Objects.requireNonNull(value), isSecret));
    }

    @InternalUse
    public OutputInternal(Deployment deployment, OutputData<T> dataFuture) {
        this(deployment, CompletableFuture.completedFuture(Objects.requireNonNull(dataFuture)));
    }

    @InternalUse
    public OutputInternal(Deployment deployment, CompletableFuture<OutputData<T>> dataFuture) {
        this.dataFuture = requireNonNull(dataFuture);
        this.deployment = requireNonNull(deployment);
        DeploymentInternal.cast(deployment).getRunner().registerTask(
                this.getClass().getTypeName() + " -> " + dataFuture, dataFuture);
    }

    @InternalUse
    public OutputInternal(Deployment deployment, Set<Resource> resources, T value) {
        this(deployment, CompletableFuture.completedFuture(
                OutputData.of(ImmutableSet.copyOf(resources), value)));
    }

    @Override
    public <U> Output<U> apply(Function<T, Output<U>> func) {
        Function<T, Output<U>> funcWithDeployment = t ->
                CurrentDeployment.withCurrentDeployment(this.deployment, () -> func.apply(t));
        return new OutputInternal<>(this.deployment, OutputData.apply(
                dataFuture,
                funcWithDeployment.andThen(o -> Internal.of(o).getDataAsync())
        ));
    }

    public Output<T> copy() {
        // we do not copy the OutputData, because it should be immutable
        return new OutputInternal<>(this.deployment, this.dataFuture.copy()); // TODO: is the copy deep enough
    }

    public Output<T> asPlaintext() { // TODO: this look very unsafe to be exposed, what are the use cases? do we need this?
        return withIsSecret(CompletableFuture.completedFuture(false));
    }

    public Output<T> asSecret() {
        return withIsSecret(CompletableFuture.completedFuture(true));
    }

    @InternalUse
    public Output<T> withIsSecret(CompletableFuture<Boolean> isSecretFuture) {
        return new OutputInternal<>(this.deployment,
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

    // Static section -----

    static <T> OutputInternal<T> cast(Output<T> output) {
        Objects.requireNonNull(output);
        if (output instanceof OutputInternal) {
            return (OutputInternal<T>) output;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Expected a 'OutputInternal<T>' instance, got: %s",
                    output.getClass().getSimpleName()
            ));
        }
    }

    @Override
    public Deployment getDeployment() {
        return deployment;
    }

    @InternalUse
    public static final Output<Tuples.Tuple0> tupleZeroOut(Output baseOutput) {
        return new OutputInternal<Tuples.Tuple0>(baseOutput.getDeployment(), Tuples.Tuple0.Empty);
    }
}
