package io.pulumi.core;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.Tuples.Tuple3;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class InputOutputTests {

    @CanIgnoreReturnValue
    public static
    <T, IO extends InputOutput<T, IO>> InputOutputData<T> waitFor(IO io) {
        return TypedInputOutput.cast(io).mutate(CompletableFuture::join);
    }

    @CanIgnoreReturnValue
    public static
    <T1, IO1 extends InputOutput<T1, IO1>, T2, IO2 extends InputOutput<T2, IO2>>
    Tuple2<InputOutputData<T1>, InputOutputData<T2>>
    waitFor(IO1 io1, IO2 io2) {
        return Tuples.of(
                TypedInputOutput.cast(io1).mutate(CompletableFuture::join),
                TypedInputOutput.cast(io2).mutate(CompletableFuture::join)
        );
    }

    @CanIgnoreReturnValue
    public static
    <T1, IO1 extends InputOutput<T1, IO1>, T2, IO2 extends InputOutput<T2, IO2>, T3, IO3 extends InputOutput<T3, IO3>>
    Tuple3<InputOutputData<T1>, InputOutputData<T2>, InputOutputData<T3>>
    waitFor(IO1 io1, IO2 io2, IO3 io3) {
        return Tuples.of(
                TypedInputOutput.cast(io1).mutate(CompletableFuture::join),
                TypedInputOutput.cast(io2).mutate(CompletableFuture::join),
                TypedInputOutput.cast(io3).mutate(CompletableFuture::join)
        );
    }

    public static <T> Output<T> unknown(T value) {
        return new OutputDefault<>(InputOutputData.ofNullable(ImmutableSet.of(), value, false, false));
    }

    public static <T> Output<T> unknown(Supplier<CompletableFuture<T>> valueFactory) {
        return new OutputDefault<>(valueFactory.get().thenApply(
                v -> InputOutputData.ofNullable(ImmutableSet.of(), v, false, false)
        ));
    }

    public static <T> Output<T> unknownSecret(T value) {
        return new OutputDefault<>(InputOutputData.ofNullable(ImmutableSet.of(), value, false, true));
    }

}
