package io.pulumi.core;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.InputOutputInternal;
import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@InternalUse
@ParametersAreNonnullByDefault
public final class OutputDefault<T> extends InputOutputInternal<T, Output<T>> implements Output<T> {

    OutputDefault(T value) {
        super(value);
    }

    OutputDefault(T value, boolean isSecret) {
        super(value, isSecret);
    }

    OutputDefault(CompletableFuture<T> value, boolean isSecret) {
        super(value, isSecret);
    }

    OutputDefault(InputOutputData<T> dataFuture) {
        super(dataFuture);
    }

    OutputDefault(CompletableFuture<InputOutputData<T>> dataFuture) {
        super(dataFuture);
    }

    @Override
    protected Output<T> newInstance(CompletableFuture<InputOutputData<T>> dataFuture) {
        return new OutputDefault<>(dataFuture);
    }

    @Deprecated
    public Input<T> toInput() {
        return new InputDefault<>(dataFuture.copy());
    }

    @Override
    public <U> Output<U> apply(Function<T, Output<U>> func) {
        return new OutputDefault<>(InputOutputData.apply(
                dataFuture,
                func.andThen(o -> Internal.of(o).getDataAsync())
        ));
    }

    // Static section -----

    @InternalUse
    public static <T> Output<T> of(Set<Resource> resources, T value) {
        Objects.requireNonNull(value);
        return new OutputDefault<>(CompletableFuture.completedFuture(
                InputOutputData.of(ImmutableSet.copyOf(resources), value)));
    }

    @InternalUse
    public static <T> Output<T> of(CompletableFuture<InputOutputData<T>> dataFuture) {
        return new OutputDefault<>(dataFuture);
    }
}
