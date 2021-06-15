package io.pulumi.core.internal;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.Internal;
import io.pulumi.core.Input;
import io.pulumi.core.InputOutput;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples.*;
import io.pulumi.deployment.Deployment;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Immutable internal type
 */
@ParametersAreNonnullByDefault
@Internal
public final class InputOutputData<T> implements Copyable<InputOutputData<T>> {
    private static final InputOutputData<?> Empty = new InputOutputData<>(ImmutableSet.of(), true, false);
    private static final InputOutputData<?> Unknown = new InputOutputData<>(ImmutableSet.of(), false, false);
    private static final InputOutputData<?> EmptySecret = new InputOutputData<>(ImmutableSet.of(), true, true);
    private static final InputOutputData<?> UnknownSecret = new InputOutputData<>(ImmutableSet.of(), false, true);

    private final ImmutableSet<Resource> resources;
    @Nullable
    private final T value;
    private final boolean known;
    private final boolean secret;

    @Internal
    private InputOutputData(ImmutableSet<Resource> resources, boolean isKnown, boolean isSecret) {
        this.resources = resources;
        this.value = null;
        this.known = isKnown;
        this.secret = isSecret;
    }

    private InputOutputData(ImmutableSet<Resource> resources, T value, boolean isKnown, boolean isSecret) {
        this.resources = Objects.requireNonNull(resources);
        this.value = Objects.requireNonNull(value);
        this.known = isKnown; // can be true even with value == null (when empty)
        this.secret = isSecret;
    }

    public static <T> InputOutputData<T> of(T value) {
        return new InputOutputData<>(ImmutableSet.of(), value, true, false);
    }

    public static <T> InputOutputData<T> of(ImmutableSet<Resource> resources, T value) {
        return new InputOutputData<>(resources, value, true, false);
    }

    public static <T> InputOutputData<T> of(ImmutableSet<Resource> resources, T value, boolean isSecret) {
        return new InputOutputData<>(resources, value, true, isSecret);
    }

    public static <T> InputOutputData<T> of(T value, boolean isSecret) {
        return new InputOutputData<>(ImmutableSet.of(), value, true, isSecret);
    }

    public static <T> CompletableFuture<InputOutputData<T>> ofAsync(CompletableFuture<T> value, boolean isSecret) {
        Objects.requireNonNull(value);
        return value.thenApply(v -> ofNullable(ImmutableSet.of(), v, true, isSecret));
    }

    public static <T> InputOutputData<T> ofNullable(
            ImmutableSet<Resource> resources, @Nullable T value, boolean isKnown, boolean isSecret
    ) {
        if (resources.isEmpty() && value == null) {
            if (isKnown && !isSecret) {
                return empty();
            }
            //noinspection ConstantConditions
            if (isKnown && isSecret) {
                return emptySecret();
            }
            //noinspection ConstantConditions
            if (!isKnown && !isSecret) {
                return unknown();
            }
            //noinspection ConstantConditions
            if (!isKnown && isSecret) {
                return unknownSecret();
            }
        }
        if (value == null) {
            // rare case, of unknown or empty value but with resources
            return new InputOutputData<>(resources, isKnown, isSecret);
        }
        return new InputOutputData<>(resources, value, isKnown, isSecret);
    }

    public static <T> InputOutputData<T> empty() {
        //noinspection unchecked
        return (InputOutputData<T>) Empty;
    }

    public static <T> InputOutputData<T> emptySecret() {
        //noinspection unchecked
        return (InputOutputData<T>) EmptySecret;
    }

    public static <T> InputOutputData<T> unknown() {
        //noinspection unchecked
        return (InputOutputData<T>) Unknown;
    }

    public static <T> InputOutputData<T> unknownSecret() {
        //noinspection unchecked
        return (InputOutputData<T>) UnknownSecret;
    }

    public InputOutputData<T> copy() {
        return ofNullable(this.resources, this.value, this.known, this.secret);
    }

    public InputOutputData<T> withIsSecret(boolean isSecret) {
        return ofNullable(this.resources, this.value, this.known, isSecret);
    }

    public <U> InputOutputData<U> apply(Function<T, U> function) {
        return ofNullable(this.resources, function.apply(this.value), this.known, this.secret);
    }

    public ImmutableSet<Resource> getResources() {
        return this.resources;
    }

    public Optional<T> getValueOptional() {
        return Optional.ofNullable(this.value);
    }

    @Nullable
    public T getValueNullable() {
        return this.value;
    }

    public Optional<T> filter(Predicate<T> isEmpty) {
        return getValueOptional().stream()
                .filter(isEmpty.negate())
                .findFirst();
    }

    public boolean isKnown() {
        return this.known;
    }

    public boolean isSecret() {
        return this.secret;
    }

    public boolean isPresent() {
        return this.value != null;
    }

    public boolean isEmpty() {
        return this.value == null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("resources", resources)
                .add("value", value)
                .add("known", known)
                .add("secret", secret)
                .toString();
    }

    public static <T, U> CompletableFuture<InputOutputData<U>> apply(
            CompletableFuture<InputOutputData<T>> dataFuture,
            Function<T, CompletableFuture<InputOutputData<U>>> func
    ) {
        return dataFuture.thenApply((InputOutputData<T> data) -> {
            ImmutableSet<Resource> resources = data.getResources();

            // TODO: reference implementation had a special case for previews here
            //       but is it really needed or can it be done differently?
            // During previews only, perform the apply only if the engine was able to
            // give us an actual value for this Output.
            if (!data.isKnown() && Deployment.getInstance().isDryRun()) {
                return CompletableFuture.completedFuture(
                        InputOutputData.ofNullable(resources, (U) null, false, data.isSecret())
                );
            }

            CompletableFuture<InputOutputData<U>> inner = func.apply(data.value);
            Objects.requireNonNull(inner);
            return inner.thenApply(innerData -> InputOutputData.ofNullable(
                    ImmutableSet.copyOf(Sets.union(resources, innerData.getResources())),
                    innerData.getValueOptional().orElse(null),
                    data.known && innerData.known,
                    data.secret || innerData.secret
            ));
        }).thenCompose(Function.identity()); // TODO: this looks ugly, what am I doing wrong?
    }

    @Internal
    public static <T> CompletableFuture<InputOutputData<List<T>>> internalAllHelperAsync(
            List<CompletableFuture<InputOutputData<T>>> values) {
        return CompletableFutures.allOf(values)
                .thenApply(vs -> {
                    List<InputOutputData<T>> dataList = vs
                            .stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    return builder(new ArrayList<T>(vs.size()))
                            .accumulate(dataList, (ts, t) -> {
                                if (t != null) {
                                    ts.add(t);
                                }
                                return ts;
                            })
                            .build(ImmutableList::copyOf);
                });
    }

    @Internal
    public static CompletableFuture<InputOutputData<Object>> internalCopyInputOutputData(@SuppressWarnings("rawtypes") @Nullable InputOutput obj) {
        if (obj == null) {
            return CompletableFuture.completedFuture(InputOutputData.empty());
        }
        //noinspection unchecked,rawtypes,rawtypes
        return ((InputOutputImpl) obj).internalGetDataAsync().copy();
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> CompletableFuture<InputOutputData<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> tuple(
            Input<T1> input1, Input<T2> input2, Input<T3> input3, Input<T4> input4,
            Input<T5> input5, Input<T6> input6, Input<T7> input7, Input<T8> input8
    ) {
        return internalTupleHelperAsync(
                (TypedInputOutput.cast(input1)).internalGetDataAsync(),
                (TypedInputOutput.cast(input2)).internalGetDataAsync(),
                (TypedInputOutput.cast(input3)).internalGetDataAsync(),
                (TypedInputOutput.cast(input4)).internalGetDataAsync(),
                (TypedInputOutput.cast(input5)).internalGetDataAsync(),
                (TypedInputOutput.cast(input6)).internalGetDataAsync(),
                (TypedInputOutput.cast(input7)).internalGetDataAsync(),
                (TypedInputOutput.cast(input8)).internalGetDataAsync()
        );
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> CompletableFuture<InputOutputData<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> tuple(
            Output<T1> output1, Output<T2> output2, Output<T3> output3, Output<T4> output4,
            Output<T5> output5, Output<T6> output6, Output<T7> output7, Output<T8> output8
    ) {
        return internalTupleHelperAsync(
                (TypedInputOutput.cast(output1)).internalGetDataAsync(),
                (TypedInputOutput.cast(output2)).internalGetDataAsync(),
                (TypedInputOutput.cast(output3)).internalGetDataAsync(),
                (TypedInputOutput.cast(output4)).internalGetDataAsync(),
                (TypedInputOutput.cast(output5)).internalGetDataAsync(),
                (TypedInputOutput.cast(output6)).internalGetDataAsync(),
                (TypedInputOutput.cast(output7)).internalGetDataAsync(),
                (TypedInputOutput.cast(output8)).internalGetDataAsync()
        );
    }

    @Internal
    private static <T1, T2, T3, T4, T5, T6, T7, T8> CompletableFuture<InputOutputData<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> internalTupleHelperAsync(
            CompletableFuture<InputOutputData<T1>> data1, CompletableFuture<InputOutputData<T2>> data2,
            CompletableFuture<InputOutputData<T3>> data3, CompletableFuture<InputOutputData<T4>> data4,
            CompletableFuture<InputOutputData<T5>> data5, CompletableFuture<InputOutputData<T6>> data6,
            CompletableFuture<InputOutputData<T7>> data7, CompletableFuture<InputOutputData<T8>> data8
    ) {
        return CompletableFuture.allOf(data1, data2, data3, data4, data5, data6, data7, data8)
                .thenApply(ignore -> builder(Tuple0.Empty)
                        .transform(data1.join(), Tuple0::append)
                        .transform(data2.join(), Tuple1::append)
                        .transform(data3.join(), Tuple2::append)
                        .transform(data4.join(), Tuple3::append)
                        .transform(data5.join(), Tuple4::append)
                        .transform(data6.join(), Tuple5::append)
                        .transform(data7.join(), Tuple6::append)
                        .transform(data8.join(), Tuple7::append)
                        .build());
    }

    @Internal
    public static <T> Builder<T> builder(T start) {
        return new Builder<>(start);
    }

    @Internal
    public static final class Builder<T> {
        private ImmutableSet.Builder<Resource> resources;
        private T value;
        private boolean isKnown;
        private boolean isSecret;

        public Builder(T start) {
            this(ImmutableSet.builder(), start, true, false);
        }

        public Builder(ImmutableSet.Builder<Resource> resources, T value, boolean isKnown, boolean isSecret) {
            this.resources = Objects.requireNonNull(resources);
            this.value = Objects.requireNonNull(value);
            this.isKnown = isKnown;
            this.isSecret = isSecret;
        }

        @CanIgnoreReturnValue
        public <E> Builder<T> accumulate(InputOutputData<E> data, BiFunction<T, E, T> reduce) {
            this.resources = this.resources.addAll(data.resources);
            this.value = reduce.apply(this.value, data.value);
            this.isKnown = this.isKnown && data.known;
            this.isSecret = this.isSecret || data.secret;
            return this;
        }

        @CanIgnoreReturnValue
        public <E> Builder<T> accumulate(Collection<InputOutputData<E>> dataCollection, BiFunction<T, E, T> reduce) {
            dataCollection.forEach(data -> accumulate(data, reduce));
            return this;
        }

        public <U, R> Builder<R> transform(InputOutputData<U> data, BiFunction<T, U, R> reduce) {
            return new Builder<>(
                    this.resources.addAll(data.resources),
                    reduce.apply(this.value, data.value),
                    this.isKnown && data.known,
                    this.isSecret || data.secret
            );
        }

        public <R> InputOutputData<R> build(Function<T, R> valuesBuilder) {
            return new InputOutputData<>(
                    this.resources.build(),
                    valuesBuilder.apply(this.value),
                    this.isKnown,
                    this.isSecret
            );
        }

        public InputOutputData<T> build() {
            return build(Function.identity());
        }
    }
}

