package io.pulumi.core;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.OutputData;
import io.pulumi.core.internal.OutputInternal;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@InternalUse
@ParametersAreNonnullByDefault
public final class OutputDefault<T> extends OutputInternal<T> implements Output<T> {

    OutputDefault(T value) {
        super(value);
    }

    OutputDefault(T value, boolean isSecret) {
        super(value, isSecret);
    }

    OutputDefault(CompletableFuture<T> value, boolean isSecret) {
        super(value, isSecret);
    }

    OutputDefault(OutputData<T> dataFuture) {
        super(dataFuture);
    }

    OutputDefault(CompletableFuture<OutputData<T>> dataFuture) {
        super(dataFuture);
    }

    @Override
    protected Output<T> newInstance(CompletableFuture<OutputData<T>> dataFuture) {
        return new OutputDefault<>(dataFuture);
    }

    @Override
    public <U> Output<U> apply(Function<T, Output<U>> func) {
        return new OutputDefault<>(OutputData.apply(
                dataFuture,
                func.andThen(o -> Internal.of(o).getDataAsync())
        ));
    }

    // Static section -----

    @InternalUse
    public static <T> Output<T> of(Set<Resource> resources, T value) {
        Objects.requireNonNull(value);
        return new OutputDefault<>(CompletableFuture.completedFuture(
                OutputData.of(ImmutableSet.copyOf(resources), value)));
    }

    @InternalUse
    public static <T> Output<T> of(CompletableFuture<OutputData<T>> dataFuture) {
        return new OutputDefault<>(dataFuture);
    }
}
