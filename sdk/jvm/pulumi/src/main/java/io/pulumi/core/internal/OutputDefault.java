package io.pulumi.core.internal;

import com.google.common.collect.ImmutableSet;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@InternalUse
@ParametersAreNonnullByDefault
public final class OutputDefault<T> extends OutputInternal<T> implements Output<T> {

    @InternalUse
    public OutputDefault(T value) {
        super(value);
    }

    @InternalUse
    public OutputDefault(T value, boolean isSecret) {
        super(value, isSecret);
    }

    @InternalUse
    public OutputDefault(CompletableFuture<T> value, boolean isSecret) {
        super(value, isSecret);
    }

    @InternalUse
    public OutputDefault(OutputData<T> dataFuture) {
        super(dataFuture);
    }

    @InternalUse
    public OutputDefault(CompletableFuture<OutputData<T>> dataFuture) {
        super(dataFuture);
    }

    @InternalUse
    public OutputDefault(Set<Resource> resources, T value) {
        super(CompletableFuture.completedFuture(OutputData.of(ImmutableSet.copyOf(resources), value)));
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
}
