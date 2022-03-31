package io.pulumi.core.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.pulumi.core.Either;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.deployment.Deployment;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;


public interface OutputBuilder {
    Deployment getDeployment();

    // default section -----

    default <T> Output<T> of() {
        return empty();
    }

    default <T> Output<T> of(T value) {
        return new OutputInternal<>(getDeployment(), value);
    }

    default <T> Output<T> of(CompletableFuture<T> value) {
        return new OutputInternal<>(getDeployment(), value, false);
    }

    default <T> Output<T> ofSecret(T value) {
        return new OutputInternal<>(getDeployment(), value, true);
    }

    default <T> Output<T> empty() {
        return new OutputInternal<>(getDeployment(), OutputData.empty());
    }

    default <T, O extends Output<T>> Output<T> ofNullable(@Nullable O value) {
        if (value == null) {
            return empty();
        }
        return value;
    }

    default <T> Output<T> ofNullable(@Nullable T value) {
        if (value == null) {
            return empty();
        }
        return of(value);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // this is a converter method, so it's ok
    default <T> Output<T> ofOptional(Optional<T> value) {
        Objects.requireNonNull(value);
        if (value.isEmpty()) {
            return empty();
        }
        return of(value.get());
    }

    /**
     * Combines all the @see {@link Output<T>} values in {@code outputs}
     * into a single @see {@link Output<T>} with an @see {@link java.util.List<T>}
     * containing all their underlying values.
     * <p/>
     * If any of the @see {@link Output<T>}s are not known, the final result will be not known.
     * Similarly, if any of the @see {@link Output<T>}s are secrets, then the final result will be a secret.
     */
    default <T> Output<List<T>> all(Output<T>... outputs) {
        return all(List.of(outputs));
    }

    /**
     * @see Output#all(Output[])  for more details.
     */
    default <T> Output<List<T>> all(Iterable<Output<T>> outputs) {
        return all(Lists.newArrayList(outputs));
    }

    @InternalUse
    default <T> Output<List<T>> all(List<Output<T>> outputs) {
        return new OutputInternal<>(
                getDeployment(),
                OutputData.allHelperAsync(outputs
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
    default Output<String> format(String formattableString,
                                  @SuppressWarnings("rawtypes") Output... arguments) {
        var data = Lists.newArrayList(arguments).stream()
                .map(OutputData::copyInputOutputData)
                .collect(Collectors.toList());

        return new OutputInternal<>(getDeployment(),
                OutputData.allHelperAsync(data)
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
    default <L, R> Output<Either<L, R>> ofLeft(L value) {
        return of(Either.ofLeft(value));
    }

    /**
     * @see #ofLeft(Object)
     */
    default <L, R> Output<Either<L, R>> ofRight(R value) {
        return of(Either.ofRight(value));
    }

    /**
     * @see #ofLeft(Object)
     */
    default <L, R> Output<Either<L, R>> ofLeft(Output<L> value) {
        return new OutputInternal<>(getDeployment(), Internal.of(value).getDataAsync()
                .thenApply(ioData -> ioData.apply(Either::<L, R>ofLeft)));
    }

    /**
     * @see #ofLeft(Object)
     */
    default <L, R> Output<Either<L, R>> ofRight(Output<R> value) {
        return new OutputInternal<>(getDeployment(), Internal.of(value).getDataAsync()
                .thenApply(ioData -> ioData.apply(Either::ofRight)));
    }

    // Convenience methods for Map

    /**
     * Returns a shallow copy of the @see {@link Map} wrapped in an @see {@link Output}
     */
    default <V> Output<Map<String, V>> copyOfMap(Map<String, V> values) {
        return of(ImmutableMap.copyOf(values));
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
    default <V> Output<Map<String, V>> concatMap(@Nullable Output<Map<String, V>> left, @Nullable Output<Map<String, V>> right) {
        return tuple(
                left == null ? of(Map.of()) : left,
                right == null ? of(Map.of()) : right
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
     * Builds an @see {@link Output} value that wraps a @see {@link Map}.
     * Also @see #ofMap(Object)
     */
    default <E> MapBuilder<E> mapBuilder() {
        return new MapBuilder<>(getDeployment());
    }

    final class MapBuilder<V> {
        private final CompletableFutures.Builder<OutputData.Builder<ImmutableMap.Builder<String, V>>> builder;
        private final Deployment deployment;

        public MapBuilder(Deployment deployment) {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(OutputData.builder(ImmutableMap.builder()))
            );
            this.deployment = deployment;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> put(String key, Output<V> value) {
            this.builder.accumulate(
                    Internal.of(value).getDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> put(String key, V value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> put(Map.Entry<? extends String, ? extends V> entry) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(entry)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::put)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public MapBuilder<V> putAll(Map<? extends String, ? extends V> map) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(map)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        @SuppressWarnings("UnstableApiUsage")
        @CanIgnoreReturnValue
        public MapBuilder<V> putAll(Iterable<? extends Map.Entry<? extends String, ? extends V>> entries) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(entries)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        public Output<Map<String, V>> build() {
            return new OutputInternal<>(this.deployment,
                    builder.build(dataBuilder -> dataBuilder.build(ImmutableMap.Builder::build)));
        }
    }

    /**
     * @see #ofMap(String, Object)
     */
    default <V> Output<Map<String, V>> ofMap() {
        return of(ImmutableMap.of());
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
    default <V> Output<Map<String, V>> ofMap(String key1, V value1) {
        return of(ImmutableMap.of(key1, value1));
    }

    /**
     * @see #ofMap(String, Object)
     */
    default <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                             String key2, V value2) {
        return of(ImmutableMap.of(key1, value1, key2, value2));
    }

    /**
     * @see #ofMap(String, Object)
     */
    default <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                             String key2, V value2,
                                             String key3, V value3) {
        return of(ImmutableMap.of(key1, value1, key2, value2, key3, value3));
    }

    /**
     * @see #ofMap(String, Object)
     */
    default <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                             String key2, V value2,
                                             String key3, V value3,
                                             String key4, V value4) {
        return of(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4));
    }

    /**
     * @see #ofMap(String, Object)
     */
    default <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                             String key2, V value2,
                                             String key3, V value3,
                                             String key4, V value4,
                                             String key5, V value5) {
        return of(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4, key5, value5));
    }



    // Convenience methods for JSON

    /**
     * @see #ofJson(JsonElement)
     */
    default Output<JsonElement> ofJson() {
        return ofJson(JsonNull.INSTANCE);
    }

    /**
     * Represents an @see {@link Output} value that wraps a @see {@link JsonElement}
     */
    default Output<JsonElement> ofJson(JsonElement json) {
       return of(json);
    }

    /**
     * @see #ofJson(JsonElement)
     */
    default Output<JsonElement> ofJson(Output<JsonElement> json) {
        return new OutputInternal<>(json.getDeployment(), Internal.of(json).getDataAsync());
    }

    /**
     * @see #ofJson(JsonElement)
     */
    default Output<JsonElement> parseJson(String json) {
        var gson = new Gson();
        return ofJson(gson.fromJson(json, JsonElement.class));
    }

    /**
     * @see #ofJson(JsonElement)
     */
    default Output<JsonElement> parseJson(Output<String> json) {
        var gson = new Gson();
        return ofJson(json.applyValue((String j) -> gson.fromJson(j, JsonElement.class)));
    }

    // Convenience methods for List

    /**
     * Returns a shallow copy of the @see {@link List} wrapped in an @see {@link Output}
     */
    default <E> Output<List<E>> copyOfList(List<E> values) {
        return of(ImmutableList.copyOf(values));
    }

    /**
     * Concatenates two lists of @see {@link Output}, can take a {@code @Nullable} that will be treated as an empty list,
     * always returns {@code non-null}.
     */
    default <E> Output<List<E>> concatList(@Nullable Output</* @Nullable */ List<E>> left, @Nullable Output</* @Nullable */List<E>> right) {
        return tuple(
                left == null ? of(List.of()) : left,
                right == null ? of(List.of()) : right
        ).applyValue(tuple -> ImmutableList.<E>builder()
                .addAll(tuple.t1 == null ? List.of() : tuple.t1)
                .addAll(tuple.t2 == null ? List.of() : tuple.t2)
                .build());
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList() {
        return of(ImmutableList.of());
    }

    /**
     * Returns an @see {@link Output} value that wraps a @see {@link List}.
     * Also @see #listBuilder()
     */
    default <E> Output<List<E>> ofList(E e1) {
        return of(ImmutableList.of(e1));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2) {
        return of(ImmutableList.of(e1, e2));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3) {
        return of(ImmutableList.of(e1, e2, e3));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4) {
        return of(ImmutableList.of(e1, e2, e3, e4));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12));
    }

    /**
     * @see #ofList(Object)
     */
    default <E> Output<List<E>> ofList(
            E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
        return of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, others));
    }

    /**
     * Builds an @see {@link Output} value that wraps a @see {@link List}.
     * Also @see #ofList(Object)
     */
    default <E> ListBuilder<E> listBuilder() {
        return new ListBuilder<>(getDeployment());
    }

    final class ListBuilder<E> {
        private final CompletableFutures.Builder<OutputData.Builder<ImmutableList.Builder<E>>> builder;

        private final Deployment deployment;

        public ListBuilder(Deployment deployment) {
            this.deployment = deployment;
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(OutputData.builder(ImmutableList.builder()))
            );
        }

        @CanIgnoreReturnValue
        public ListBuilder<E> add(Output<E> value) {
            this.builder.accumulate(
                    Internal.of(value).getDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::add)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public ListBuilder<E> add(E value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(value)),
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
                    CompletableFuture.completedFuture(OutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public ListBuilder<E> addAll(Iterator<? extends E> elements) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(OutputData.of(elements)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableList.Builder::addAll)
            );
            return this;
        }

        public Output<List<E>> build() {
            return new OutputInternal<>(this.deployment,
                    builder.build(dataBuilder -> dataBuilder.build(ImmutableList.Builder::build)));
        }
    }

    // Tuple Overloads that take various number of outputs.

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    default <T1, T2> Output<Tuples.Tuple2<T1, T2>> tuple(Output<T1> item1, Output<T2> item2) {
        var TupleZeroOut = of(Tuples.Tuple0.Empty);
        return tuple(item1, item2, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    default <T1, T2, T3> Output<Tuples.Tuple3<T1, T2, T3>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3
    ) {
        var TupleZeroOut = of(Tuples.Tuple0.Empty);
        return tuple(item1, item2, item3, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    default <T1, T2, T3, T4> Output<Tuples.Tuple4<T1, T2, T3, T4>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4
    ) {
        var TupleZeroOut = of(Tuples.Tuple0.Empty);
        return tuple(item1, item2, item3, item4, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    default <T1, T2, T3, T4, T5> Output<Tuples.Tuple5<T1, T2, T3, T4, T5>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5
    ) {
        var TupleZeroOut = of(Tuples.Tuple0.Empty);
        return tuple(item1, item2, item3, item4, item5, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    default <T1, T2, T3, T4, T5, T6> Output<Tuples.Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6
    ) {
        var TupleZeroOut = of(Tuples.Tuple0.Empty);
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    default <T1, T2, T3, T4, T5, T6, T7> Output<Tuples.Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7
    ) {
        var TupleZeroOut = of(Tuples.Tuple0.Empty);
        return tuple(item1, item2, item3, item4, item5, item6, item7, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6, v.t7));
    }

    /**
     * Combines all the @see {@link Output} values in the provided parameters and combines
     * them all into a single tuple containing each of their underlying values.
     * If any of the @see {@link Output}s are not known, the final result will be not known.  Similarly,
     * if any of the @see {@link Output}s are secrets, then the final result will be a secret.
     */
    default <T1, T2, T3, T4, T5, T6, T7, T8> Output<Tuples.Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7, Output<T8> item8
    ) {
        return new OutputInternal<>(getDeployment(),
                OutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }

    static OutputBuilder forDeployment(Deployment deployment) {
        return new SimpleOutputBuilder(deployment);
    }

    class SimpleOutputBuilder implements OutputBuilder {
        private final Deployment deployment;

        public SimpleOutputBuilder(Deployment deployment) {
            this.deployment = deployment;
        }

        @Override
        public Deployment getDeployment() {
            return deployment;
        }
    }
}
