package io.pulumi.core.internal;

import io.grpc.Internal;
import io.pulumi.core.Input;
import io.pulumi.core.InputOutput;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.deployment.internal.DeploymentInternal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@Internal
public abstract class InputOutputImpl<T, IO extends InputOutput<T, IO> & Copyable<IO>>
        extends TypedInputOutput<T, IO> implements InputOutput<T, IO> {

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
                    this.getClass().getTypeName() + " -> " + dataFuture, dataFuture
            );
        }
    }

    public IO copy() {
        // we do not copy the OutputData, because it should be immutable
        return newInstance(this.dataFuture.copy()); // TODO: is the copy deep enough
    }

    public IO asPlaintext() { // TODO: this look very unsafe to be exposed, what are the use cases? do we need this?
        return internalWithIsSecret(CompletableFuture.completedFuture(false));
    }

    public IO asSecret() {
        return internalWithIsSecret(CompletableFuture.completedFuture(true));
    }

    // TODO: replace with mutator
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

    // TODO: replace with mutator/viewer
    @Internal
    public CompletableFuture<InputOutputData<T>> internalGetDataAsync() {
        return this.dataFuture;
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
