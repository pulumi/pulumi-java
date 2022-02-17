package io.pulumi.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.InputOutputImpl;
import io.pulumi.core.internal.TypedInputOutput;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.Resource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.pulumi.core.internal.InputOutputData.internalAllHelperAsync;

@InternalUse
@ParametersAreNonnullByDefault
public final class OutputDefault<T> extends InputOutputImpl<T, Output<T>> implements Output<T> {

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

    public Input<T> toInput() {
        return new InputDefault<>(dataFuture.copy());
    }

    @Override
    public <U> Output<U> apply(Function<T, Output<U>> func) {
        return new OutputDefault<>(InputOutputData.apply(dataFuture, func.andThen(
                o -> TypedInputOutput.cast(o).internalGetDataAsync())));
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

    /**
     * Takes in a "formattableString" with potential @see {@link Input}s or @see {@link Output}
     * in the 'placeholder holes'. Conceptually, this method unwraps all the underlying values in the holes,
     * combines them appropriately with the "formattableString", and produces an @see {@link Output}
     * containing the final result.
     * <p>
     * If any of the @see {@link Input}s or {@link Output}s are not known, the
     * final result will be not known.
     * <p>
     * Similarly, if any of the @see {@link Input}s or @see {@link Input}s are secrets,
     * then the final result will be a secret.
     */
    public static Output<String> format(String formattableString, @SuppressWarnings("rawtypes") InputOutput... arguments) {
        var data = Lists.newArrayList(arguments)
                .stream()
                .map(InputOutputData::internalCopyInputOutputData)
                .collect(Collectors.toList());

        return new OutputDefault<>(
                internalAllHelperAsync(data)
                        .thenApply(objs -> objs.apply(
                                v -> v == null ? null : String.format(formattableString, v.toArray())))
        );
    }

}
