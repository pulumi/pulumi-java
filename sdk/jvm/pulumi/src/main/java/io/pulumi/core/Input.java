package io.pulumi.core;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.pulumi.core.internal.CompletableFutures;
import io.pulumi.core.internal.Copyable;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.pulumi.core.internal.InputOutputImpl.TupleZeroIn;

public interface Input<T> extends InputOutput<T, Input<T>> {

    /**
     * Convert @see {@link Input<T>} to @see {@link Output<T>}
     *
     * @return an {@link Output<T>} , converted from {@link Input<T>}
     */
    Output<T> toOutput();

    /**
     * Transforms the data of this @see {@link Input<T>} with the provided {@code func}.
     * The result remains an @see {@link Input<T>} so that dependent resources
     * can be properly tracked.
     * <p/>
     * {@code func} is not allowed to make resources.
     * <p/>
     * {@code func} can return other @see {@link Input<T>}s.  This can be handy if
     * you have an <code>Input&lt;SomeType&gt;</code> and you want to get a transitive dependency of it.  i.e.:
     * <br/>
     * <code>
     * Input&lt;SomeType&gt; d1 = ...;
     * Input&lt;OtherType&gt; d2 = d1.apply(v -> v.otherOutput); // getting an input off of 'v'.
     * </code>
     * <p/>
     * In this example, taking a dependency on d2 means a resource will depend on all the resources
     * of d1. It will <b>not</b> depend on the resources of v.x.y.OtherDep.
     * <p/>
     * Importantly, the Resources that d2 feels like it will depend on are the same resources
     * as d1.
     * <p/>
     * If you need have multiple @see {@link Input<T>}s and a single @see {@link Input<T>}
     * is needed that combines both set of resources, then @see {@link Input#allInputs(Input[])}
     * or {@link Input#tuple(Input, Input, Input)} should be used instead.
     * <p/>
     * This function will only be called during execution of a <code>pulumi up</code> request.
     * It will not run during <code>pulumi preview</code>
     * (as the values of resources are of course not known then).
     */
    <U> Input<U> apply(Function<T, Input<U>> func);

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyValue(Function<T, U> func) {
        return apply(t -> Input.of(func.apply(t)));
    }

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyOptional(Function<T, Optional<U>> func) {
        return apply(t -> Input.ofOptional(func.apply(t)));
    }

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyFuture(Function<T, CompletableFuture<U>> func) {
        return apply(t -> Input.of(func.apply(t)));
    }

    /**
     * @see Input#apply(Function) for more details.
     */
    default <U> Input<U> applyOutput(Function<T, Output<U>> func) {
        return apply(t -> func.apply(t).toInput());
    }

    @CanIgnoreReturnValue
    default Input<Void> applyVoid(Consumer<T> consumer) {
        return apply(t -> {
            consumer.accept(t);
            return Input.empty();
        });
    }

    // Static section -----

    static <T> Input<T> of() {
        return Input.empty();
    }

    static <T> Input<T> of(T value) {
        return new InputDefault<>(value);
    }

    static <T> Input<T> of(CompletableFuture<T> value) {
        return new InputDefault<>(value, false);
    }

    static <T> Input<T> ofSecret(T value) {
        return new InputDefault<>(value, true);
    }

    static <T> Input<T> empty() {
        return new InputDefault<>(InputOutputData.empty());
    }

    static <T, I extends Input<T>> Input<T> ofNullable(@Nullable I value) {
        if (value == null) {
            return Input.empty();
        }
        return value;
    }

    static <T> Input<T> ofNullable(@Nullable T value) {
        if (value == null) {
            return Input.empty();
        }
        return Input.of(value);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // this is a converter method, so it's ok
    static <T> Input<T> ofOptional(Optional<T> value) {
        Objects.requireNonNull(value);
        if (value.isEmpty()) {
            return Input.empty();
        }
        return Input.of(value.get());
    }

    // Convenience methods for Either (a.k.a. Union)

    // TODO: maybe we can move this complexity to the codegen, since this is not very useful for an end user anyway
    /**
     * Represents an @see {@link Input} value that can be one of two different types.
     * For example, it might potentially be an "Integer" some of the time
     * or a "String" in other cases.
     */
    static <L, R> Input<Either<L, R>> ofLeft(L value) {
        return Input.of(Either.ofLeft(value));
    }

    /**
     * @see #ofLeft(Object)
     */
    static <L, R> Input<Either<L, R>> ofRight(R value) {
        return Input.of(Either.ofRight(value));
    }

    /**
     * @see #ofLeft(Object)
     */
    static <L, R> Input<Either<L, R>> ofLeft(Output<L> value) {
        return new InputDefault<>(TypedInputOutput.cast(value).internalGetDataAsync()
                .thenApply(ioData -> ioData.apply(Either::<L, R>ofLeft)));
    }

    /**
     * @see #ofLeft(Object)
     */
    static <L, R> Input<Either<L, R>> ofRight(Output<R> value) {
        return new InputDefault<>(TypedInputOutput.cast(value).internalGetDataAsync()
                .thenApply(ioData -> ioData.apply(Either::ofRight)));
    }

    // Convenience methods for JSON

    /**
     * @see #ofJson(JsonElement)
     */
    static Input<JsonElement> ofJson() {
        return ofJson(JsonNull.INSTANCE);
    }

    /**
     * Represents an @see {@link Input} value that wraps a @see {@link JsonElement}
     */
    static Input<JsonElement> ofJson(JsonElement json) {
        return Input.of(json);
    }

    /**
     * @see #ofJson(JsonElement)
     */
    static Input<JsonElement> ofJson(Output<JsonElement> json) {
        return new InputDefault<>(TypedInputOutput.cast(json).internalGetDataAsync());
    }

    /**
     * @see #ofJson(JsonElement)
     */
    static Input<JsonElement> parseJson(String json) {
        var gson = new Gson();
        return ofJson(gson.fromJson(json, JsonElement.class));
    }

    /**
     * @see #ofJson(JsonElement)
     */
    static Input<JsonElement> parseJson(Output<String> json) {
        var gson = new Gson();
        return ofJson(json.applyValue((String j) -> gson.fromJson(j, JsonElement.class)));
    }

    // Convenience methods for List

    /**
     * Returns a shallow copy of the @see {@link List} wrapped in an @see {@link Input}
     */
    static <E> Input<List<E>> copyOfList(List<E> values) {
        return Input.of(ImmutableList.copyOf(values));
    }

    /**
     * Concatenates two lists of @see {@link Input}, can take a {@code @Nullable}, returns {@code non-null}.
     */
    static <E> Input<List<E>> concatList(@Nullable Input</* @Nullable */ List<E>> left, @Nullable Input</* @Nullable */List<E>> right) {
        if (left == null && right == null) {
            return Input.empty();
        }
        if (left == null) {
            left = Input.empty();
        }
        if (right == null) {
            right = Input.empty();
        }

        return concatListInternal(left, right);
    }

    private static <E> Input<List<E>> concatListInternal(Input</* @Nullable */ List<E>> left, Input</* @Nullable */List<E>> right) {
        return Input.of(TypedInputOutput.cast(left).view(InputOutputData::isEmpty).thenCompose(
                leftIsEmpty -> TypedInputOutput.cast(right).view(InputOutputData::isEmpty).thenCompose(
                        rightIsEmpty -> TypedInputOutput.cast(left).view(InputOutputData::getValueNullable).thenCompose(
                                l -> TypedInputOutput.cast(right).view(InputOutputData::getValueNullable).thenApply(
                                        r -> {
                                            if (leftIsEmpty && rightIsEmpty) {
                                                return null;
                                            }
                                            return Stream
                                                    .concat(
                                                            (l == null ? ImmutableList.<E>of() : l).stream(),
                                                            (r == null ? ImmutableList.<E>of() : r).stream()
                                                    )
                                                    .collect(toImmutableList());
                                        }
                                )
                        )
                )
        ));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList() {
        return Input.of(ImmutableList.of());
    }

    /**
     * Returns an @see {@link Input} value that wraps a @see {@link List}.
     * Also @see #listBuilder()
     */
    static <E> Input<List<E>> ofList(E e1) {
        return Input.of(ImmutableList.of(e1));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2) {
        return Input.of(ImmutableList.of(e1, e2));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3) {
        return Input.of(ImmutableList.of(e1, e2, e3));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Input<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12));
    }

    /**
     * @see #ofList(Object)
     */
    @SafeVarargs
    static <E> Input<List<E>> ofList(
            E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
        return Input.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, others));
    }

    /**
     * Builds an @see {@link Input} value that wraps a @see {@link List}.
     * Also @see #ofList(Object)
     */
    static <E> ListBuilder<E> listBuilder() {
        return new ListBuilder<>();
    }

    final class ListBuilder<E> {
        private final CompletableFutures.Builder<InputOutputData.Builder<ImmutableList.Builder<E>>> builder;

        public ListBuilder() {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(InputOutputData.builder(ImmutableList.builder()))
            );
        }

        @CanIgnoreReturnValue
        public <IO extends InputOutput<E, IO> & Copyable<IO>> ListBuilder<E> add(InputOutput<E, IO> value) {
            this.builder.accumulate(
                    TypedInputOutput.cast(value).internalGetDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public ListBuilder<E> add(E value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }


        @SafeVarargs
        @CanIgnoreReturnValue
        public final ListBuilder<E> add(E... elements) {
            return addAll(List.of(elements));
        }

        @CanIgnoreReturnValue
        public ListBuilder<E> addAll(Iterable<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public ListBuilder<E> addAll(Iterator<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        public Input<List<E>> build() {
            return new InputDefault<>(builder.build(dataBuilder -> dataBuilder.build(ImmutableList.Builder::build)));
        }
    }

    // Convenience methods for Map

    /**
     * Returns a shallow copy of the @see {@link Map} wrapped in an @see {@link Input}
     */
    static <V> Input<Map<String, V>> copyOfMap(Map<String, V> values) {
        return Input.of(ImmutableMap.copyOf(values));
    }

    /**
     * Concatenates two @see {@link Map} wrapped in an @see {@link Input}.
     * Returns a new instance without modifying any of the arguments.
     * <p/>
     * If both maps contain the same key, the value from the second map takes over.
     *
     * @param left  The first @see {@code Input<Map<V>>}
     * @param right The second @see {@code Input<Map<V>>}, it has higher priority in case of key clash.
     * @return A new instance of {@code Input<Map<V>>} that contains the items from both input maps.
     */
    static <V> Input<Map<String, V>> concatMap(@Nullable Input<Map<String, V>> left, @Nullable Input<Map<String, V>> right) {
        if (left == null && right == null) {
            return Input.empty();
        }
        if (left == null) {
            left = Input.empty();
        }
        if (right == null) {
            right = Input.empty();
        }

        return concatMapInternal(left, right);
    }

    private static <V> Input<Map<String, V>> concatMapInternal(Input<Map<String, V>> left, Input<Map<String, V>> right) {
        return Input.of(TypedInputOutput.cast(left).view(InputOutputData::isEmpty).thenCompose(
                leftIsEmpty -> TypedInputOutput.cast(right).view(InputOutputData::isEmpty).thenCompose(
                        rightIsEmpty -> TypedInputOutput.cast(left).view(InputOutputData::getValueNullable).thenCompose(
                                l -> TypedInputOutput.cast(right).view(InputOutputData::getValueNullable).thenApply(
                                        r -> {
                                            if (leftIsEmpty && rightIsEmpty) {
                                                return null;
                                            }
                                            return Stream
                                                    .concat(
                                                            (l == null ? ImmutableMap.<String, V>of() : l).entrySet().stream(),
                                                            (r == null ? ImmutableMap.<String, V>of() : r).entrySet().stream()
                                                    )
                                                    .collect(toImmutableMap(
                                                            Map.Entry::getKey,
                                                            Map.Entry::getValue,
                                                            (v1, v2) -> v2 // in case of duplicate, ignore the v1
                                                    ));
                                        }
                                )
                        )
                )
        ));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Input<Map<String, V>> ofMap() {
        return Input.of(ImmutableMap.of());
    }

    /**
     * Returns an @see {@link Input} value that wraps a @see {@link Map}.
     * </p>
     * A mapping of {@code String}s to values that can be passed in as the arguments to
     * a @see {@link io.pulumi.resources.Resource}.
     * The individual values are themselves @see {@link Input<V>}s.
     * <p/>
     *
     * {@code Input<Map<String,V>>} differs from a normal @see {@link Map} in that it is
     * wrapped in an @see {@link Input<V>}. For example, a @see {@link io.pulumi.resources.Resource}
     * that accepts an {@code Input<Map<String,V>>} may accept not just a map but an @see {@link Output}
     * of a map as well.
     * This is important for cases where the @see {@link Output}
     * map from some {@link io.pulumi.resources.Resource} needs to be passed
     * into another {@link io.pulumi.resources.Resource}.
     * Or for cases where creating the map invariably produces an {@link Output} because
     * its resultant value is dependent on other {@link Output}s.
     * <p/>
     * This benefit of {@code Input<Map<String,V>>} is also a limitation. Because it represents
     * a list of values that may eventually be created, there is no way to simply iterate over,
     * or access the elements of the map synchronously.
     * <p/>
     * {@code Input<Map<String,V>>} is designed to be easily used in object and collection initializers.
     * For example, a resource that accepts a map of values can be written easily in this form:
     * <p/>
     * <code>
     * new SomeResource("name", new SomeResourceArgs(
     *   Input.ofMap(
     *     key1, value1,
     *     key2, value2,
     *     key3, value3,
     *   )
     * ));
     * </code>
     * </p>
     * Also @see #mapBuilder()
     */
    static <V> Input<Map<String, V>> ofMap(String key1, V value1) {
        return Input.of(ImmutableMap.of(key1, value1));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Input<Map<String, V>> ofMap(String key1, V value1,
                                           String key2, V value2) {
        return Input.of(ImmutableMap.of(key1, value1, key2, value2));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Input<Map<String, V>> ofMap(String key1, V value1,
                                           String key2, V value2,
                                           String key3, V value3) {
        return Input.of(ImmutableMap.of(key1, value1, key2, value2, key3, value3));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Input<Map<String, V>> ofMap(String key1, V value1,
                                           String key2, V value2,
                                           String key3, V value3,
                                           String key4, V value4) {
        return Input.of(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Input<Map<String, V>> ofMap(String key1, V value1,
                                           String key2, V value2,
                                           String key3, V value3,
                                           String key4, V value4,
                                           String key5, V value5) {
        return Input.of(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4, key5, value5));
    }

    /**
     * Builds an @see {@link Input} value that wraps a @see {@link Map}.
     * Also @see #ofMap(Object)
     */
    static <E> MapBuilder<E> mapBuilder() {
        return new MapBuilder<>();
    }

    final class MapBuilder<V> {
        private final CompletableFutures.Builder<InputOutputData.Builder<ImmutableMap.Builder<String, V>>> builder;

        public MapBuilder() {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(InputOutputData.builder(ImmutableMap.builder()))
            );
        }

        @CanIgnoreReturnValue
        public <IO extends InputOutput<V, IO> & Copyable<IO>> MapBuilder<V> put(String key, InputOutput<V, IO> value) {
            this.builder.accumulate(
                    TypedInputOutput.cast(value).internalGetDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> put(String key, V value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> put(Map.Entry<? extends String, ? extends V> entry) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(entry)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::put)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> putAll(Map<? extends String, ? extends V> map) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(map)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        @Beta
        @CanIgnoreReturnValue
        public MapBuilder<V> putAll(Iterable<? extends Map.Entry<? extends String, ? extends V>> entries) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(entries)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        public Input<Map<String, V>> build() {
            return new InputDefault<>(builder.build(dataBuilder -> dataBuilder.build(ImmutableMap.Builder::build)));
        }
    }

    // Tuple Overloads that take different numbers of inputs or outputs.

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2> Input<Tuples.Tuple2<T1, T2>> tuple(Input<T1> item1, Input<T2> item2) {
        return tuple(item1, item2, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3> Input<Tuples.Tuple3<T1, T2, T3>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3
    ) {
        return tuple(item1, item2, item3, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4> Input<Tuples.Tuple4<T1, T2, T3, T4>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4
    ) {
        return tuple(item1, item2, item3, item4, TupleZeroIn, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5> Input<Tuples.Tuple5<T1, T2, T3, T4, T5>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4, Input<T5> item5
    ) {
        return tuple(item1, item2, item3, item4, item5, TupleZeroIn, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5, T6> Input<Tuples.Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4, Input<T5> item5, Input<T6> item6
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroIn, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @see Input#tuple(Input, Input, Input, Input, Input, Input, Input, Input)
     */
    static <T1, T2, T3, T4, T5, T6, T7> Input<Tuples.Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4,
            Input<T5> item5, Input<T6> item6, Input<T7> item7
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, item7, TupleZeroIn)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6, v.t7));
    }

    /**
     * Combines all the @see {@link Input} values in the provided parameters and combines
     * them all into a single tuple containing each of their underlying values.
     * If any of the @see {@link Input}s are not known, the final result will be not known.  Similarly,
     * if any of the @see {@link Input}s are secrets, then the final result will be a secret.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Input<Tuples.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Input<T1> item1, Input<T2> item2, Input<T3> item3, Input<T4> item4,
            Input<T5> item5, Input<T6> item6, Input<T7> item7, Input<T8> item8
    ) {
        return new InputDefault<>(InputOutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }
}
