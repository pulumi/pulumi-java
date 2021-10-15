package io.pulumi.core.internal;

import io.grpc.Internal;
import io.pulumi.core.Input;
import io.pulumi.core.InputOutput;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.deployment.internal.DeploymentInternal;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@Internal
public abstract class InputOutputImpl<T, IO extends InputOutput<T, IO> & Copyable<IO>>
        implements InputOutput<T, IO>, TypedInputOutput<T, IO>, UntypedInputOutput {

    @Internal
    public static final Input<Tuples.Tuple0> TupleZeroIn = Input.of(Tuples.Tuple0.Empty);
    @Internal
    public static final Output<Tuples.Tuple0> TupleZeroOut = Output.of(Tuples.Tuple0.Empty);

    protected final CompletableFuture<InputOutputData<T>> dataFuture;

    protected InputOutputImpl(T value) {
        this(value, false);
    }

    protected InputOutputImpl(T value, boolean isSecret) {
        this(CompletableFuture.completedFuture(Objects.requireNonNull(value)), isSecret);
    }

    protected InputOutputImpl(CompletableFuture<T> value, boolean isSecret) {
        this(InputOutputData.ofAsync(Objects.requireNonNull(value), isSecret));
    }

    protected InputOutputImpl(InputOutputData<T> dataFuture) {
        this(CompletableFuture.completedFuture(Objects.requireNonNull(dataFuture)));
    }

    protected InputOutputImpl(CompletableFuture<InputOutputData<T>> dataFuture) {
        this.dataFuture = Objects.requireNonNull(dataFuture);

        var deployment = DeploymentInternal.getInstanceOptional();
        if (deployment.isPresent()) {
            deployment.get().getRunner().registerTask(
                    this.getClass().getTypeName(), dataFuture
            );
        }
    }

    protected abstract IO newInstance(CompletableFuture<InputOutputData<T>> dataFuture);

    public IO copy() {
        // we do not copy the OutputData, because it should be immutable
        return newInstance(this.dataFuture.copy()); // TODO: is the copy deep enough
    }

    public IO unsecret() { // TODO: this look very unsafe to be exposed, what are the use cases? do we need this?
        return internalWithIsSecret(CompletableFuture.completedFuture(false));
    }

    public IO secretify() {
        return internalWithIsSecret(CompletableFuture.completedFuture(true));
    }

    /**
     * @return the secret-ness status of the given output
     */
    @Internal
    public CompletableFuture<Boolean> internalIsSecret() {
        return this.dataFuture.thenApply(InputOutputData::isSecret);
    }

    @Internal
    public IO internalWithIsSecret(CompletableFuture<Boolean> isSecretFuture) {
        return newInstance(
                isSecretFuture.thenCompose(
                        secret -> this.dataFuture.thenApply(
                                d -> d.withIsSecret(secret)
                        )
                )
        );
    }

    @Internal
    public CompletableFuture<InputOutputData<T>> internalGetDataAsync() {
        return this.dataFuture;
    }

    @Internal
    public CompletableFuture<Optional<T>> internalGetValueOptionalAsync() {
        return this.dataFuture.thenApply(InputOutputData::getValueOptional);
    }

    @Override
    @Internal
    public CompletableFuture<Set<Resource>> internalGetResourcesUntypedAsync() {
        return this.dataFuture.thenApply(InputOutputData::getResources);
    }

    @Override
    @Internal
    public CompletableFuture<InputOutputData<Object>> internalGetDataUntypedAsync() {
        return this.dataFuture.thenApply(inputOutputData -> inputOutputData.apply(t -> t));
    }

    @Override
    @Internal
    public <U> U mutate(Mutator<T, U> mutator) {
        return mutator.mutate(this.dataFuture);
    }

    @Override
    @Internal
    public <U> CompletableFuture<U> view(Viewer<T, U> viewer) {
        return this.dataFuture.thenApply(viewer::view);
    }

    // Static section -------
}
