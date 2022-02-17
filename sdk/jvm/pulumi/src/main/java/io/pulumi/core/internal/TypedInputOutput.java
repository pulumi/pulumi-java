package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.InputOutput;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.Resource;
import io.pulumi.serialization.internal.OutputCompletionSource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Internal interface to allow our code to operate on inputs/outputs in a typed manner.
 */
@ParametersAreNonnullByDefault
@InternalUse
public abstract class TypedInputOutput<T, IO extends InputOutput<T, IO> & Copyable<IO>> {

    protected abstract IO newInstance(CompletableFuture<InputOutputData<T>> dataFuture);

    public abstract CompletableFuture<InputOutputData<T>> internalGetDataAsync();

    public abstract IO internalWithIsSecret(CompletableFuture<Boolean> isSecretFuture);

    public static <T, IO extends InputOutput<T, IO> & Copyable<IO>> TypedInputOutput<T, IO> cast(
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

    public abstract <U> CompletableFuture<U> view(Viewer<T, U> viewer);

    public interface Viewer<T, U> {
        U view(InputOutputData<T> dataFuture);
    }

    public abstract <U> U mutate(Mutator<T, U> mutator);

    public interface Mutator<T, U> {
        U mutate(CompletableFuture<InputOutputData<T>> dataFuture);
    }

    public interface ConsumingMutator<T> extends Mutator<T, Void> {
        @CanIgnoreReturnValue
        @Override
        Void mutate(CompletableFuture<InputOutputData<T>> dataFuture);
    }

    public static <T, IO extends InputOutput<T, IO> & Copyable<IO>> OutputCompletionSource<T> outputCompletionSource(
            InputOutput<T, IO> inputOutput, ImmutableSet<Resource> resources, TypeShape<?> fieldTypeShape
    ) {
        var mutator = new OutputCompletionMutator<T>(resources, fieldTypeShape);
        TypedInputOutput.cast(inputOutput).mutate(mutator);
        return mutator;
    }

    @ParametersAreNonnullByDefault
    static class OutputCompletionMutator<T> implements OutputCompletionSource<T>, ConsumingMutator<T> {

        protected final ImmutableSet<Resource> resources;
        protected final TypeShape<?> fieldTypeShape;
        protected CompletableFuture<InputOutputData<T>> mutableData;

        public OutputCompletionMutator(ImmutableSet<Resource> resources, TypeShape<?> fieldTypeShape) {
            this.resources = Objects.requireNonNull(resources);
            this.fieldTypeShape = Objects.requireNonNull(fieldTypeShape);
        }

        @CanIgnoreReturnValue
        @Override
        public Void mutate(CompletableFuture<InputOutputData<T>> data) {
            this.mutableData = Objects.requireNonNull(data);
            //noinspection RedundantCast
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
            //noinspection unchecked
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
                            data.getValueNullable(),
                            data.isKnown(),
                            data.isSecret()
                    ));
        }

        @Override
        public TypeShape<?> getTypeShape() {
            return fieldTypeShape;
        }
    }
}
