package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.Internal;
import io.pulumi.core.InputOutput;
import io.pulumi.resources.Resource;
import io.pulumi.serialization.internal.OutputCompletionSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Internal interface to allow our code to operate on inputs/outputs in a typed manner.
 */
@ParametersAreNonnullByDefault
@Internal
public interface TypedInputOutput<T, IO extends InputOutput<T, IO> & Copyable<IO>> {

    CompletableFuture<InputOutputData<T>> internalGetDataAsync();

    CompletableFuture<Boolean> internalIsSecret();

    IO internalWithIsSecret(CompletableFuture<Boolean> isSecretFuture);

    static <T, IO extends InputOutput<T, IO> & Copyable<IO>> TypedInputOutput<T, IO> cast(
            InputOutput<T, IO> inputOutput
    ) {
        Objects.requireNonNull(inputOutput);
        if (inputOutput instanceof TypedInputOutput) {
            //noinspection unchecked
            return (TypedInputOutput<T, IO>) inputOutput;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Expected a 'TypedInputOutput<T>' instance, got: %s",
                    inputOutput.getClass().getSimpleName()
            ));
        }
    }

    <U> CompletableFuture<U> view(Viewer<T, U> viewer);

    interface Viewer<T, U> {
        U view(InputOutputData<T> dataFuture);
    }

    <U> U mutate(Mutator<T, U> mutator);

    interface Mutator<T, U> {
        U mutate(CompletableFuture<InputOutputData<T>> dataFuture);
    }

    interface ConsumingMutator<T> extends Mutator<T, Void> {
        @CanIgnoreReturnValue
        @Override
        Void mutate(CompletableFuture<InputOutputData<T>> dataFuture);
    }

    static <T, IO extends InputOutput<T, IO> & Copyable<IO>> OutputCompletionSource<T> outputCompletionSource(
            InputOutput<T, IO> inputOutput, ImmutableSet<Resource> resources
    ) {
        var mutator = new OutputCompletionMutator<T>(resources);
        TypedInputOutput.cast(inputOutput).mutate(mutator);
        return mutator;
    }

    @ParametersAreNonnullByDefault
    class OutputCompletionMutator<T> implements OutputCompletionSource<T>, ConsumingMutator<T> {

        protected final ImmutableSet<Resource> resources;
        protected CompletableFuture<InputOutputData<T>> mutableData;

        public OutputCompletionMutator(ImmutableSet<Resource> resources) {
            this.resources = Objects.requireNonNull(resources);
        }

        @CanIgnoreReturnValue
        @Override
        public Void mutate(CompletableFuture<InputOutputData<T>> data) {
            this.mutableData = Objects.requireNonNull(data);
            return (Void) null;
        }

        @Override
        public void trySetException(Exception exception) {
            mutableData.completeExceptionally(exception);
        }

        @Override
        public void trySetDefaultResult(boolean isKnown) {
            mutableData.complete(InputOutputData.ofNullable(
                    ImmutableSet.of(), null, isKnown, false) // TODO: check if this does not break things later on
            );
        }

        @Override
        public void setStringValue(String value, boolean isKnown) {
            mutableData.complete(InputOutputData.ofNullable(
                    this.resources,
                    (T) value,
                    isKnown,
                    false
            ));
        }

        @Override
        public void setValue(InputOutputData<T> data) {
            mutableData
                    .complete(InputOutputData.ofNullable(
                            Sets.union(this.resources, data.getResources()).immutableCopy(),
                            data.getValueOptional().orElse(null),
                            data.isKnown(),
                            data.isSecret()
                    ));
        }
    }
}
