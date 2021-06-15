package io.pulumi.core;

import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an @see {@link Input} value that can be one of two different types.
 * For example, it might potentially be an "Integer" some of the time
 * or a "String" in other cases.
 */
public final class InputUnion<L, R> extends InputImpl<Either<L, R>, Input<Either<L, R>>> implements Input<Either<L, R>> {

    private InputUnion(Either<L, R> oneOf) {
        super(oneOf, false);
    }

    private InputUnion(CompletableFuture<Either<L, R>> future, boolean isSecret) {
        super(future, isSecret);
    }

    private InputUnion(CompletableFuture<InputOutputData<Either<L, R>>> dataFuture) {
        super(dataFuture);
    }

    @Override
    protected Input<Either<L, R>> newInstance(CompletableFuture<InputOutputData<Either<L, R>>> dataFuture) {
        return new InputUnion<>(dataFuture);
    }

    @Override
    public <U> Input<U> apply(Function<Either<L, R>, Input<U>> func) {
        return new InputDefault<>(InputOutputData.apply(dataFuture, func.andThen(
                o -> TypedInputOutput.cast(o).internalGetDataAsync())
        ));
    }

    // Static section -----

    public static <L, R> InputUnion<L, R> leftOf(L value) {
        return new InputUnion<>(Either.leftOf(value));
    }

    public static <L, R> InputUnion<L, R> rightOf(R value) {
        return new InputUnion<>(Either.rightOf(value));
    }

    public static <L, R> InputUnion<L, R> leftOf(Output<L> value) {
        return new InputUnion<>(TypedInputOutput.cast(value).internalGetDataAsync()
                .thenApply(ioData -> ioData.apply(Either::leftOf)));
    }

    public static <L, R> InputUnion<L, R> right(Output<R> value) {
        return new InputUnion<>(TypedInputOutput.cast(value).internalGetDataAsync()
                .thenApply(ioData -> ioData.apply(Either::rightOf)));
    }
}