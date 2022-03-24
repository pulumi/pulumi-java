package io.pulumi.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.pulumi.core.internal.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static io.pulumi.core.internal.OutputData.allHelperAsync;
import static io.pulumi.core.internal.OutputInternal.TupleZeroOut;

public interface Output<T> extends Copyable<Output<T>> {

    /**
     * Transforms the data of this @see {@link Output<T>} with the provided {@code func}.
     * The result remains an @see {@link Output<T>} so that dependent resources
     * can be properly tracked.
     * <p/>
     * {@code func} is not allowed to make resources.
     * <p/>
     * {@code func} can return other @see {@link Output<T>}s.  This can be handy if
     * you have an <code>Output&lt;SomeType&gt;</code> and you want to get a transitive dependency of it.  i.e.:
     * <br/>
     * <code>
     * Output&lt;SomeType&gt; d1 = ...;
     * Output&lt;OtherType&gt; d2 = d1.apply(v -> v.otherOutput); // getting an output off of 'v'.
     * </code>
     * <p/>
     * In this example, taking a dependency on d2 means a resource will depend on all the resources
     * of d1. It will <b>not</b> depend on the resources of v.x.y.OtherDep.
     * <p/>
     * Importantly, the Resources that d2 feels like it will depend on are the same resources
     * as d1.
     * <p/>
     * If you need have multiple @see {@link Output<T>}s and a single @see {@link Output<T>}
     * is needed that combines both set of resources, then @see {@link Output#all(Output[])}
     * or {@link Output#tuple(Output, Output, Output)} should be used instead.
     * <p/>
     * This function will only be called during execution of a <code>pulumi up</code> request.
     * It will not run during <code>pulumi preview</code>
     * (as the values of resources are of course not known then).
     */
    <U> Output<U> apply(Function<T, Output<U>> func);

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyValue(Function<T, U> func) {
        return apply(t -> Output.of(func.apply(t)));
    }

    /**
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyFuture(Function<T, CompletableFuture<U>> func) {
        return apply(t -> Output.ofFuture(func.apply(t)));
    }

    @CanIgnoreReturnValue
    default Output<Void> applyVoid(Consumer<T> consumer) {
        return apply(t -> {
            consumer.accept(t);
            return Output.of(null);
        });
    }

    /**
     * Creates a shallow copy (the underlying CompletableFuture is copied) of this @see {@link Output<T>}
     * @return a shallow copy of the @see {@link Output<T>}
     */
    Output<T> copy();

    /**
     * Returns a new @see {@link Output<T>} which is a copy of the existing output but marked as
     * a non-secret. The original output or input is not modified in any way.
     */
    Output<T> asPlaintext();

    /**
     * Returns a new @see {@link Output<T>} which is a copy of the existing output but marked as
     * a secret. The original output or input is not modified in any way.
     */
    Output<T> asSecret();

    // Static section -----

    static <T> Output<T> of(T value) {
        return new OutputInternal<>(value);
    }

    static <T> Output<T> ofFuture(CompletableFuture<T> value) {
        return new OutputInternal<>(value, false);
    }

    static <T> Output<T> ofSecret(T value) {
        return new OutputInternal<>(value, true);
    }

    static <T> Output<T> unknown() {
        return new OutputInternal<>(OutputData.unknown());
    }

    /**
     * Combines all the @see {@link Output<T>} values in {@code outputs}
     * into a single @see {@link Output<T>} with an @see {@link java.util.List<T>}
     * containing all their underlying values.
     * <p/>
     * If any of the @see {@link Output<T>}s are not known, the final result will be not known.
     * Similarly, if any of the @see {@link Output<T>}s are secrets, then the final result will be a secret.
     */
    @SafeVarargs // safe because we only call List.of, that is also @SafeVarargs
    static <T> Output<List<T>> all(Output<T>... outputs) {
        return all(List.of(outputs));
    }

    /**
     * @see Output#all(Output[])  for more details.
     */
    static <T> Output<List<T>> all(Iterable<Output<T>> outputs) {
        return all(Lists.newArrayList(outputs));
    }

    private static <T> Output<List<T>> all(List<Output<T>> outputs) {
        return new OutputInternal<>(
                allHelperAsync(outputs
                        .stream()
                        .map(output -> Internal.of(output).getDataAsync())
                        .collect(Collectors.toList()))
        );
    }

    /**
     * Takes in a "formattableString" with potential @see {@link Output}
     * in the 'placeholder holes'. Conceptually, this method unwraps all the underlying values in the holes,
     * combines them appropriately with the "formattableString", and produces an @see {@link Output}
     * containing the final result.
     * <p>
     * If any of the {@link Output}s are not known, the
     * final result will be not known.
     * <p>
     * Similarly, if any of the @see {@link Output}s are secrets,
     * then the final result will be a secret.
     */
    static Output<String> format(String formattableString, @SuppressWarnings("rawtypes") Output... arguments) {
        var data = Lists.newArrayList(arguments).stream()
                .map(OutputData::copyInputOutputData)
                .collect(Collectors.toList());

        return new OutputInternal<>(
                allHelperAsync(data)
                        .thenApply(objs -> objs.apply(
                                v -> v == null ? null : String.format(formattableString, v.toArray())))
        );
    }

    // Convenience methods for Either (a.k.a. Union)

    // TODO: maybe we can move this complexity to the codegen, since this is not very useful for an end user anyway

    /**
     * Represents an @see {@link Output} value that can be one of two different types.
     * For example, it might potentially be an "Integer" some of the time
     * or a "String" in other cases.
     */
    static <L, R> Output<Either<L, R>> ofLeft(L value) {
        return Output.of(Either.ofLeft(value));
    }

    /**
     * @see #ofLeft(Object)
     */
    static <L, R> Output<Either<L, R>> ofRight(R value) {
        return Output.of(Either.ofRight(value));
    }

    /**
     * @see #ofLeft(Object)
     */
    static <L, R> Output<Either<L, R>> ofLeft(Output<L> value) {
        return new OutputInternal<>(Internal.of(value).getDataAsync()
                .thenApply(ioData -> ioData.apply(Either::<L, R>ofLeft)));
    }

    /**
     * @see #ofLeft(Object)
     */
    static <L, R> Output<Either<L, R>> ofRight(Output<R> value) {
        return new OutputInternal<>(Internal.of(value).getDataAsync()
                .thenApply(ioData -> ioData.apply(Either::ofRight)));
    }

    // Convenience methods for JSON

    /**
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> ofJson() {
        return ofJson(JsonNull.INSTANCE);
    }

    /**
     * Represents an @see {@link Output} value that wraps a @see {@link JsonElement}
     */
    static Output<JsonElement> ofJson(JsonElement json) {
        return Output.of(json);
    }

    /**
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> ofJson(Output<JsonElement> json) {
        return new OutputInternal<>(Internal.of(json).getDataAsync());
    }

    /**
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> parseJson(String json) {
        var gson = new Gson();
        return ofJson(gson.fromJson(json, JsonElement.class));
    }

    /**
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> parseJson(Output<String> json) {
        var gson = new Gson();
        return ofJson(json.applyValue((String j) -> gson.fromJson(j, JsonElement.class)));
    }

    // Convenience methods for List

    /**
     * Returns a shallow copy of the @see {@link List} wrapped in an @see {@link Output}
     */
    static <E> Output<List<E>> copyOfList(List<E> values) {
        return Output.of(ImmutableList.copyOf(values));
    }

    /**
     * Concatenates two lists of @see {@link Output}, can take a {@code @Nullable} that will be treated as an empty list,
     * always returns {@code non-null}.
     */
    static <E> Output<List<E>> concatList(@Nullable Output</* @Nullable */ List<E>> left, @Nullable Output</* @Nullable */List<E>> right) {
        return tuple(
                left == null ? Output.of(List.of()) : left,
                right == null ? Output.of(List.of()) : right
        ).applyValue(tuple -> ImmutableList.<E>builder()
                .addAll(tuple.t1 == null ? List.of() : tuple.t1)
                .addAll(tuple.t2 == null ? List.of() : tuple.t2)
                .build());
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList() {
        return Output.of(ImmutableList.of());
    }

    /**
     * Returns an @see {@link Output} value that wraps a @see {@link List}.
     * Also @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1) {
        return Output.of(ImmutableList.of(e1));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2) {
        return Output.of(ImmutableList.of(e1, e2));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3) {
        return Output.of(ImmutableList.of(e1, e2, e3));
    }


    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
    }

    /**
     * @see #ofList(Object)
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12));
    }

    /**
     * @see #ofList(Object)
     */
    @SafeVarargs
    static <E> Output<List<E>> ofList(
            E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, others));
    }

    /**
     * Builds an @see {@link Output} value that wraps a @see {@link List}.
     * Also @see #ofList(Object)
     */
    static <E> Output.ListBuilder<E> listBuilder() {
        return new Output.ListBuilder<>();
    }

    final class ListBuilder<E> {
        private final CompletableFutures.Builder<OutputData.Builder<ImmutableList.Builder<E>>> builder;

        public ListBuilder() {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(OutputData.builder(ImmutableList.builder()))
            );
        }

        @CanIgnoreReturnValue
        public Output.ListBuilder<E> add(Output<E> value) {
            this.builder.accumulate(
                    Internal.of(value).getDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public Output.ListBuilder<E> add(E value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }


        @SafeVarargs
        @CanIgnoreReturnValue
        public final Output.ListBuilder<E> add(E... elements) {
            return addAll(List.of(elements));
        }

        @CanIgnoreReturnValue
        public Output.ListBuilder<E> addAll(Iterable<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public Output.ListBuilder<E> addAll(Iterator<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        public Output<List<E>> build() {
            return new OutputInternal<>(builder.build(dataBuilder -> dataBuilder.build(ImmutableList.Builder::build)));
        }
    }

    // Convenience methods for Map

    /**
     * Returns a shallow copy of the @see {@link Map} wrapped in an @see {@link Output}
     */
    static <V> Output<Map<String, V>> copyOfMap(Map<String, V> values) {
        return Output.of(ImmutableMap.copyOf(values));
    }

    /**
     * Concatenates two @see {@link Map} wrapped in an @see {@link Output}.
     * Returns a new instance without modifying any of the arguments.
     * <p/>
     * If both maps contain the same key, the value from the second map takes over.
     * <p/>
     * Null values in the Output or Map layer are treated as empty maps.
     *
     * @param left  The first @see {@code Output<Map<V>>}
     * @param right The second @see {@code Output<Map<V>>}, it has higher priority in case of key clash.
     * @return A new instance of {@code Output<Map<V>>} that contains the items from both input maps.
     */
    static <V> Output<Map<String, V>> concatMap(@Nullable Output<Map<String, V>> left, @Nullable Output<Map<String, V>> right) {
        return tuple(
                left == null ? Output.of(Map.of()) : left,
                right == null ? Output.of(Map.of()) : right
        ).applyValue(tuple ->
                Stream.concat(
                        (tuple.t1 == null ? ImmutableMap.<String, V>of() : tuple.t1).entrySet().stream(),
                        (tuple.t2 == null ? ImmutableMap.<String, V>of() : tuple.t2).entrySet().stream()
                ).collect(toImmutableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v2 // in case of duplicate, ignore the v1
                )));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Output<Map<String, V>> ofMap() {
        return Output.of(ImmutableMap.of());
    }

    /**
     * Returns an @see {@link Output} value that wraps a @see {@link Map}.
     * </p>
     * A mapping of {@code String}s to values that can be passed in as the arguments to
     * a @see {@link io.pulumi.resources.Resource}.
     * The individual values are themselves @see {@link Output<V>}s.
     * <p/>
     * <p>
     * {@code Output<Map<String,V>>} differs from a normal @see {@link Map} in that it is
     * wrapped in an @see {@link Output<V>}. For example, a @see {@link io.pulumi.resources.Resource}
     * that accepts an {@code Output<Map<String,V>>} may accept not just a map but an @see {@link Output}
     * of a map as well.
     * This is important for cases where the @see {@link Output}
     * map from some {@link io.pulumi.resources.Resource} needs to be passed
     * into another {@link io.pulumi.resources.Resource}.
     * Or for cases where creating the map invariably produces an {@link Output} because
     * its resultant value is dependent on other {@link Output}s.
     * <p/>
     * This benefit of {@code Output<Map<String,V>>} is also a limitation. Because it represents
     * a list of values that may eventually be created, there is no way to simply iterate over,
     * or access the elements of the map synchronously.
     * <p/>
     * {@code Output<Map<String,V>>} is designed to be easily used in object and collection initializers.
     * For example, a resource that accepts a map of values can be written easily in this form:
     * <p/>
     * <code>
     * new SomeResource("name", new SomeResourceArgs(
     * Output.ofMap(
     * key1, value1,
     * key2, value2,
     * key3, value3,
     * )
     * ));
     * </code>
     * </p>
     * Also @see #mapBuilder()
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1) {
        return Output.of(ImmutableMap.of(key1, value1));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                            String key2, V value2) {
        return Output.of(ImmutableMap.of(key1, value1, key2, value2));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                            String key2, V value2,
                                            String key3, V value3) {
        return Output.of(ImmutableMap.of(key1, value1, key2, value2, key3, value3));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                            String key2, V value2,
                                            String key3, V value3,
                                            String key4, V value4) {
        return Output.of(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4));
    }

    /**
     * @see #ofMap(String, Object)
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                            String key2, V value2,
                                            String key3, V value3,
                                            String key4, V value4,
                                            String key5, V value5) {
        return Output.of(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4, key5, value5));
    }

    /**
     * Builds an @see {@link Output} value that wraps a @see {@link Map}.
     * Also @see #ofMap(Object)
     */
    static <E> Output.MapBuilder<E> mapBuilder() {
        return new Output.MapBuilder<>();
    }

    final class MapBuilder<V> {
        private final CompletableFutures.Builder<OutputData.Builder<ImmutableMap.Builder<String, V>>> builder;

        public MapBuilder() {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(OutputData.builder(ImmutableMap.builder()))
            );
        }

        @CanIgnoreReturnValue
        public Output.MapBuilder<V> put(String key, Output<V> value) {
            this.builder.accumulate(
                    Internal.of(value).getDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public Output.MapBuilder<V> put(String key, V value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public Output.MapBuilder<V> put(Map.Entry<? extends String, ? extends V> entry) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(entry)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::put)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public Output.MapBuilder<V> putAll(Map<? extends String, ? extends V> map) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(map)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        @SuppressWarnings("UnstableApiUsage")
        @CanIgnoreReturnValue
        public Output.MapBuilder<V> putAll(Iterable<? extends Map.Entry<? extends String, ? extends V>> entries) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(entries)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        public Output<Map<String, V>> build() {
            return new OutputInternal<>(builder.build(dataBuilder -> dataBuilder.build(ImmutableMap.Builder::build)));
        }
    }

    // Tuple Overloads that take various number of outputs.

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2> Output<Tuples.Tuple2<T1, T2>> tuple(Output<T1> item1, Output<T2> item2) {
        return tuple(item1, item2, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3> Output<Tuples.Tuple3<T1, T2, T3>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3
    ) {
        return tuple(item1, item2, item3, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4> Output<Tuples.Tuple4<T1, T2, T3, T4>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4
    ) {
        return tuple(item1, item2, item3, item4, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5> Output<Tuples.Tuple5<T1, T2, T3, T4, T5>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5
    ) {
        return tuple(item1, item2, item3, item4, item5, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5, T6> Output<Tuples.Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5, T6, T7> Output<Tuples.Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, item7, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6, v.t7));
    }

    /**
     * Combines all the @see {@link Output} values in the provided parameters and combines
     * them all into a single tuple containing each of their underlying values.
     * If any of the @see {@link Output}s are not known, the final result will be not known.  Similarly,
     * if any of the @see {@link Output}s are secrets, then the final result will be a secret.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Output<Tuples.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7, Output<T8> item8
    ) {
        return new OutputInternal<>(OutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }
}
