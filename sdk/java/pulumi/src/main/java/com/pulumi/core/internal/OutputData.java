package com.pulumi.core.internal;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pulumi.core.Output;
import com.pulumi.core.Tuples.Tuple0;
import com.pulumi.core.Tuples.Tuple1;
import com.pulumi.core.Tuples.Tuple2;
import com.pulumi.core.Tuples.Tuple3;
import com.pulumi.core.Tuples.Tuple4;
import com.pulumi.core.Tuples.Tuple5;
import com.pulumi.core.Tuples.Tuple6;
import com.pulumi.core.Tuples.Tuple7;
import com.pulumi.core.Tuples.Tuple8;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Immutable internal data type
 */
@ParametersAreNonnullByDefault
@InternalUse
public final class OutputData<T> implements Copyable<OutputData<T>> {
    private static final OutputData<?> Empty = new OutputData<>(ImmutableSet.of(), null, true, false);
    private static final OutputData<?> Unknown = new OutputData<>(ImmutableSet.of(), null, false, false);
    private static final OutputData<?> EmptySecret = new OutputData<>(ImmutableSet.of(), null, true, true);
    private static final OutputData<?> UnknownSecret = new OutputData<>(ImmutableSet.of(), null, false, true);

    private final ImmutableSet<Resource> resources;
    @Nullable
    private final T value;
    private final boolean known;
    private final boolean secret;

    private OutputData(ImmutableSet<Resource> resources, @Nullable T value, boolean isKnown, boolean isSecret) {
        this.resources = requireNonNull(resources);
        if (!isKnown && value != null) {
            throw new IllegalArgumentException(String.format("Expected an 'unknown' OutputData to not carry a non-null value, but got: '%s'", value));
        }
        this.value = value;
        this.known = isKnown; // can be true even with value == null (when empty)
        this.secret = isSecret;
    }

    /**
     * @throws NullPointerException on {@code null} value
     */
    public static <T> OutputData<T> of(T value) {
        return new OutputData<>(ImmutableSet.of(), requireNonNull(value), true, false);
    }

    /**
     * @throws NullPointerException on {@code null} value
     */
    public static <T> OutputData<T> of(ImmutableSet<Resource> resources, T value) {
        return new OutputData<>(resources, requireNonNull(value), true, false);
    }

    /**
     * @throws NullPointerException on {@code null} value
     */
    public static <T> OutputData<T> of(ImmutableSet<Resource> resources, T value, boolean isSecret) {
        return new OutputData<>(resources, requireNonNull(value), true, isSecret);
    }

    /**
     * @throws NullPointerException on {@code null} value
     */
    public static <T> OutputData<T> of(T value, boolean isSecret) {
        return new OutputData<>(ImmutableSet.of(), requireNonNull(value), true, isSecret);
    }

    /**
     * @throws NullPointerException on {@code null} value of the future, but not the value that future holds
     */
    public static <T> CompletableFuture<OutputData<T>> ofAsync(CompletableFuture<T> value, boolean isSecret) {
        requireNonNull(value);
        return value.thenApply(v -> ofNullable(ImmutableSet.of(), v, true, isSecret));
    }

    public static <T> OutputData<T> ofNullable(@Nullable T value) {
        return ofNullable(ImmutableSet.of(), value, true, false);
    }

    public static <T> OutputData<T> ofNullable(ImmutableSet<Resource> resources, @Nullable T value) {
        return ofNullable(resources, value, true, false);
    }

    public static <T> OutputData<T> ofNullable(ImmutableSet<Resource> resources, @Nullable T value, boolean isSecret) {
        return ofNullable(resources, value, true, isSecret);
    }

    public static <T> OutputData<T> ofNullable(
            ImmutableSet<Resource> resources, @Nullable T value, boolean isKnown, boolean isSecret
    ) {
        // Identify special cases to reuse singleton objects. Everything should use this factory method to
        // construct instances to take advantage of this optimization.
        if (resources.isEmpty() && value == null) {
            if (isKnown && !isSecret) {
                return (OutputData<T>) Empty;
            }
            //noinspection ConstantConditions
            if (isKnown && isSecret) {
                return (OutputData<T>) EmptySecret;
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
        return new OutputData<>(resources, value, isKnown, isSecret);
    }

    public static <T> OutputData<T> unknown() {
        //noinspection unchecked
        return (OutputData<T>) Unknown;
    }

    public static <T> OutputData<T> unknownSecret() {
        //noinspection unchecked
        return (OutputData<T>) UnknownSecret;
    }

    public OutputData<T> copy() {
        return ofNullable(this.resources, this.value, this.known, this.secret);
    }

    public OutputData<T> withIsSecret(boolean isSecret) {
        return ofNullable(this.resources, this.value, this.known, isSecret);
    }

    public OutputData<T> withDependency(Resource resource) {
        var newDependencies = Sets.union(
                this.resources,
                ImmutableSet.of(resource)
        ).immutableCopy();
        return ofNullable(newDependencies, this.value, this.known, this.secret);
    }

    public OutputData<T> withDependencies(List<Resource> resources) {
        var newDependencies = Sets.union(
            this.resources,
            ImmutableSet.copyOf(resources)
        ).immutableCopy();
        return ofNullable(newDependencies, this.value, this.known, this.secret);
    }

    public <U> OutputData<U> apply(Function<? super T, ? extends U> function) {
        if (known) {
            return ofNullable(resources, function.apply(value), true, secret);
        } else {
            return ofNullable(resources, null, false, secret);
        }
    }

    public <U, V> OutputData<V> combine(OutputData<? extends U> other,
                                        BiFunction<? super T, ? super U, ? extends V> fn) {
        var combinedResources = ImmutableSet.<Resource>builder()
                .addAll(this.resources)
                .addAll(other.resources)
                .build();
        var combinedSecret = secret || other.isSecret();
        if (known && other.known) {
            var combinedValue = fn.apply(value, other.value);
            return ofNullable(combinedResources, combinedValue, true, combinedSecret);
        } else {
            return ofNullable(combinedResources, null, false, combinedSecret);
        }
    }

    public <U> OutputData<U> compose(Function<T, OutputData<U>> function) {
        if (known) {
            return combine(function.apply(value), (__, x) -> x);
        } else {
            return ofNullable(resources, null, false, secret);
        }
    }

    public ImmutableSet<Resource> getResources() {
        return this.resources;
    }

    public Optional<T> getValueOptional() {
        return Optional.ofNullable(this.value);
    }

    @Nullable
    public T getValueOrDefault(@Nullable T defaultValue) {
        return getValueOptional().orElse(defaultValue);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutputData<?> that = (OutputData<?>) o;
        return known == that.known
                && secret == that.secret
                && resources.equals(that.resources)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, value, known, secret);
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

    public <V> CompletableFuture<OutputData<V>> traverseFuture(Function<T, CompletableFuture<V>> fn) {
        if (known) {
            return fn.apply(value).thenApply(x -> apply(__ -> x));
        } else {
            return CompletableFuture.completedFuture(ofNullable(resources, null, false, secret));
        }
    }

    public static <T, U> CompletableFuture<OutputData<U>> apply(
            CompletableFuture<OutputData<T>> dataFuture,
            Function<T, CompletableFuture<OutputData<U>>> func) {
        return dataFuture.thenCompose(outputData ->
                outputData
                        .traverseFuture(func)
                        .thenApply(nested -> nested.compose(data -> data)));
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> CompletableFuture<OutputData<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> tuple(
            Output<T1> output1, Output<T2> output2, Output<T3> output3, Output<T4> output4,
            Output<T5> output5, Output<T6> output6, Output<T7> output7, Output<T8> output8
    ) {
        return tupleHelperAsync(
                (Internal.of(output1)).getDataAsync(),
                (Internal.of(output2)).getDataAsync(),
                (Internal.of(output3)).getDataAsync(),
                (Internal.of(output4)).getDataAsync(),
                (Internal.of(output5)).getDataAsync(),
                (Internal.of(output6)).getDataAsync(),
                (Internal.of(output7)).getDataAsync(),
                (Internal.of(output8)).getDataAsync()
        );
    }

    @InternalUse
    private static <T1, T2, T3, T4, T5, T6, T7, T8> CompletableFuture<OutputData<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>> tupleHelperAsync(
            CompletableFuture<OutputData<T1>> data1, CompletableFuture<OutputData<T2>> data2,
            CompletableFuture<OutputData<T3>> data3, CompletableFuture<OutputData<T4>> data4,
            CompletableFuture<OutputData<T5>> data5, CompletableFuture<OutputData<T6>> data6,
            CompletableFuture<OutputData<T7>> data7, CompletableFuture<OutputData<T8>> data8) {
        return CompletableFuture.allOf(data1, data2, data3, data4, data5, data6, data7, data8)
                .thenApply(ignore -> builder(Tuple0.Empty)
                        .<T1, Tuple1<T1>>transform(data1.join(), Tuple0::append)
                        .<T2, Tuple2<T1, T2>>transform(data2.join(), Tuple1::append)
                        .<T3, Tuple3<T1, T2, T3>>transform(data3.join(), Tuple2::append)
                        .<T4, Tuple4<T1, T2, T3, T4>>transform(data4.join(), Tuple3::append)
                        .<T5, Tuple5<T1, T2, T3, T4, T5>>transform(data5.join(), Tuple4::append)
                        .<T6, Tuple6<T1, T2, T3, T4, T5, T6>>transform(data6.join(), Tuple5::append)
                        .<T7, Tuple7<T1, T2, T3, T4, T5, T6, T7>>transform(data7.join(), Tuple6::append)
                        .<T8, Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>transform(data8.join(), Tuple7::append)
                        .build());
    }

    @InternalUse
    public static <T> Builder<T> builder(@Nullable T start) {
        return new Builder<>(start);
    }

    @InternalUse
    public static final class Builder<T> {
        private OutputData<T> value;

        public Builder(@Nullable T start) {
            this(OutputData.ofNullable(start));
        }

        public Builder(OutputData<T> start) {
            value = start;
        }

        @CanIgnoreReturnValue
        public <E> Builder<T> accumulate(OutputData<E> data, BiFunction<T, E, T> reduce) {
            value = value.combine(data, reduce);
            return this;
        }

        @CanIgnoreReturnValue
        public <E> Builder<T> accumulate(Collection<OutputData<E>> dataCollection, BiFunction<T, E, T> reduce) {
            dataCollection.forEach(data -> accumulate(data, reduce));
            return this;
        }

        @CanIgnoreReturnValue
        public <E> Builder<T> accumulate(Stream<OutputData<E>> dataCollection, BiFunction<T, E, T> reduce) {
            dataCollection.forEach(data -> accumulate(data, reduce));
            return this;
        }

        public <U, R> Builder<R> transform(OutputData<U> data, BiFunction<T, U, R> reduce) {
            return new Builder<>(value.combine(data, reduce));
        }

        public <R> OutputData<R> build(Function<T, R> valuesBuilder) {
            return this.value.apply(valuesBuilder);
        }

        public OutputData<T> build() {
            return build(Function.identity());
        }
    }
}
