package io.pulumi.core;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.CompletableFutures;
import io.pulumi.core.internal.Copyable;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class InputList<E> extends InputImpl<List<E>, Input<List<E>>> implements Input<List<E>> {

    protected InputList() {
        super(ImmutableList.of());
    }

    protected InputList(List<E> values) {
        super(values, false);
    }

    protected InputList(Input<List<E>> inputs) {
        super(TypedInputOutput.cast(inputs).internalGetDataAsync().copy());
    }

    protected InputList(CompletableFuture<InputOutputData<List<E>>> values) {
        super(values);
    }

    protected InputList<E> newInstance(CompletableFuture<InputOutputData<List<E>>> dataFuture) {
        return new InputList<>(dataFuture);
    }

    @Override
    public <U> Input<U> apply(Function<List<E>, Input<U>> func) {
        return new InputDefault<>(InputOutputData.apply(
                dataFuture,
                func.andThen(input -> TypedInputOutput.cast(input).internalGetDataAsync())
        ));
    }

    @Override
    public InputList<E> copy() {
        return new InputList<>(this.dataFuture.copy());
    }

    public InputList<E> concat(InputList<E> other) {
        Objects.requireNonNull(other);

        return new InputList<>(
                Input.tuple(this, other).applyValue(
                        t -> Stream
                                .concat(t.t1.stream(), t.t2.stream())
                                .collect(toImmutableList())
                )
        );
    }

    // Static section -----

    public static <E> InputList<E> copyOf(List<E> values) {
        return new InputList<>(ImmutableList.copyOf(values));
    }

    public static <E> InputList<E> empty() {
        return new InputList<>();
    }

    public static <E> InputList<E> of() {
        return new InputList<>(ImmutableList.of());
    }

    public static <E> InputList<E> of(E e1) {
        return new InputList<>(ImmutableList.of(e1));
    }

    public static <E> InputList<E> of(E e1, E e2) {
        return new InputList<>(ImmutableList.of(e1, e2));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3) {
        return new InputList<>(ImmutableList.of(e1, e2, e3));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
    }

    public static <E> InputList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12));
    }

    @SafeVarargs
    public static <E> InputList<E> of(
            E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
        return new InputList<>(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, others));
    }

    public static <E> InputList.Builder<E> builder() {
        return new InputList.Builder<>();
    }

    public static final class Builder<E> {
        private final CompletableFutures.Builder<InputOutputData.Builder<ImmutableList.Builder<E>>> builder;

        public Builder() {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(InputOutputData.builder(ImmutableList.builder()))
            );
        }

        @CanIgnoreReturnValue
        public <IO extends InputOutput<E, IO> & Copyable<IO>> InputList.Builder<E> add(InputOutput<E, IO> value) {
            this.builder.accumulate(
                    TypedInputOutput.cast(value).internalGetDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public InputList.Builder<E> add(E value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }


        @SafeVarargs
        @CanIgnoreReturnValue
        public final InputList.Builder<E> add(E... elements) {
            return addAll(List.of(elements));
        }

        @CanIgnoreReturnValue
        public InputList.Builder<E> addAll(Iterable<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public InputList.Builder<E> addAll(Iterator<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        public InputList<E> build() {
            return new InputList<>(builder.build(dataBuilder -> dataBuilder.build(ImmutableList.Builder::build)));
        }
    }
}