package io.pulumi.core;

import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.InputOutputImpl;
import io.pulumi.core.internal.TypedInputOutput;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@InternalUse
public final class InputDefault<T> extends InputOutputImpl<T, Input<T>> implements Input<T> {

    InputDefault(T value) {
        super(value);
    }

    InputDefault(T value, boolean isSecret) {
        super(value, isSecret);
    }

    InputDefault(CompletableFuture<T> value, boolean isSecret) {
        super(value, isSecret);
    }

    InputDefault(InputOutputData<T> dataFuture) {
        super(dataFuture);
    }

    InputDefault(CompletableFuture<InputOutputData<T>> dataFuture) {
        super(dataFuture);
    }

    @Override
    protected Input<T> newInstance(CompletableFuture<InputOutputData<T>> dataFuture) {
        return new InputDefault<>(dataFuture);
    }

    public Output<T> toOutput() {
        return new OutputDefault<>(this.dataFuture.copy());
    }

    @Override
    public <U> Input<U> apply(Function<T, Input<U>> func) {
        return new InputDefault<>(InputOutputData.apply(this.dataFuture, func.andThen(
                o -> TypedInputOutput.cast(o).internalGetDataAsync())));
    }

    // Static section -----
}
