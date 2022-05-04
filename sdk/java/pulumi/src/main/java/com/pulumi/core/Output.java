package com.pulumi.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.pulumi.core.Tuples.Tuple2;
import com.pulumi.core.Tuples.Tuple3;
import com.pulumi.core.Tuples.Tuple4;
import com.pulumi.core.Tuples.Tuple5;
import com.pulumi.core.Tuples.Tuple6;
import com.pulumi.core.Tuples.Tuple7;
import com.pulumi.core.Tuples.Tuple8;
import com.pulumi.core.internal.CompletableFutures;
import com.pulumi.core.internal.Copyable;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Lists.newArrayList;
import static com.pulumi.core.internal.OutputData.allHelperAsync;
import static com.pulumi.core.internal.OutputInternal.TupleZeroOut;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

/**
 * {@code Output}{@literal <T>} is a key part of how Pulumi tracks dependencies
 * between {@link com.pulumi.resources.Resource}s.
 * Because the values of outputs are not available until resources are created,
 * these are represented using the special {@code Output}{@literal <T>} type,
 * which internally represents two things:
 * <ol>
 *     <li>An eventually available value of the output</li>
 *     <li>The dependency on the source(s) of the output value</li>
 * </ol>
 * In fact, {@code Output}{@literal <T>} is quite similar to {@link CompletableFuture}.
 * Additionally, they carry along dependency information.
 * <p>
 * The output properties of all resource objects in Pulumi have type {@code Output}{@literal <T>}.
 */
public interface Output<T> extends Copyable<Output<T>> {

    /**
     * Transforms the data of this {@link Output}{@literal <T>} with the provided {@code func}.
     * The result remains an {@link Output}{@literal <T>} so that dependent resources
     * can be properly tracked.
     * <p>
     * {@code func} is not allowed to make resources.
     * <p>
     * {@code func} can return other {@link Output}{@literal <T>}s.  This can be handy if
     * you have an <code>Output&lt;SomeType&gt;</code> and you want to get a transitive dependency of it.  i.e.:
     * <br>
     * <code>
     * Output&lt;SomeType&gt; d1 = ...;
     * Output&lt;OtherType&gt; {@literal d2 = d1.apply(v -> v.otherOutput);} // getting an output off of 'v'.
     * </code>
     * <p>
     * In this example, taking a dependency on d2 means a resource will depend on all the resources
     * of d1. It will <b>not</b> depend on the resources of v.x.y.OtherDep.
     * <p>
     * Importantly, the Resources that d2 feels like it will depend on are the same resources
     * as d1.
     * <p>
     * If you need have multiple {@link Output}{@literal <T>}s and a single {@link Output}{@literal <T>}
     * is needed that combines both set of resources, then {@link Output#all(Output[])}
     * or {@link Output#tuple(Output, Output, Output)} should be used instead.
     * <p>
     * This function will only be called during execution of a <code>pulumi up</code> request.
     * It will not run during <code>pulumi preview</code>
     * (as the values of resources are of course not known then).
     *
     * @param <U>  type of the {@link Output} returned by {@code func}
     * @param func the function to apply to the current value
     * @return an {@link Output} after applying {@code func}
     */
    <U> Output<U> apply(Function<T, Output<U>> func);

    /**
     * @param <U>  type returned by {@code func}
     * @param func the function to apply to the current value
     * @return an {@link Output} after applying {@code func}
     * @see Output#apply(Function) for more details.
     */
    default <U> Output<U> applyValue(Function<T, U> func) {
        return apply(t -> Output.ofNullable(func.apply(t)));
    }

    /**
     * Creates a shallow copy (the underlying CompletableFuture is copied) of this {@link Output}{@literal <T>}
     *
     * @return a shallow copy of the {@link Output}{@literal <T>}
     */
    Output<T> copy();

    /**
     * Returns a new {@link Output}{@literal <T>} which is a copy of the existing output but marked as
     * a non-secret. The original output or input is not modified in any way.
     * <p><b>Please use with caution</b></p>
     *
     * @return this {@link Output} as a non-secret
     */
    Output<T> asPlaintext();

    /**
     * Returns a new {@link Output}{@literal <T>} which is a copy of the existing output but marked as
     * a secret. The original output or input is not modified in any way.
     *
     * @return this {@link Output} as a secret
     */
    Output<T> asSecret();

    // Static section -----

    /**
     * Returns an {@code Output}{@literal <T>} describing the given non-{@code null} value.
     *
     * @param value the value to describe, which must be non-{@code null}
     * @param <T>   the type of the value
     * @return an {@code Output}{@literal <T>} with the value present
     * @throws NullPointerException if value is {@code null}
     */
    static <T> Output<T> of(T value) {
        requireNonNull(value);
        return new OutputInternal<>(value);
    }

    /**
     * Returns an {@code Output}{@literal <T>} describing a future value.
     *
     * @param future the future to describe, which must be non-{@code null}
     * @param <T>    the type of the value
     * @return an {@code Output}{@literal <T>} with the value present
     * @throws NullPointerException if future is {@code null}, but not the future value
     */
    static <T> Output<T> of(CompletableFuture<T> future) {
        return new OutputInternal<>(future, false);
    }

    /**
     * Returns an {@code Output}{@literal <T>} describing the given non-{@code null} secret value.
     *
     * @param value the secret value to describe, which must be non-{@code null}
     * @param <T>   the type of the value
     * @return an {@code Output}{@literal <T>} with the value present
     * @throws NullPointerException if value is {@code null}
     */
    static <T> Output<T> ofSecret(T value) {
        return new OutputInternal<>(value, true);
    }

    /**
     * Returns an {@code Output}{@literal <T>} describing the given value, if
     * non-{@code null}, otherwise returns an empty {@code Output}{@literal <T>}.
     *
     * @param value the possibly-{@code null} value to describe
     * @param <T>   the type of the value
     * @return an {@code Output}{@literal <T>} with a present value if the specified value
     * is non-{@code null}, otherwise an empty {@code Output}{@literal <T>}
     */
    static <T> Output<T> ofNullable(@Nullable T value) {
        return new OutputInternal<>(value);
    }

    /**
     * Combines all the {@link Output}{@literal <T>} values in {@code outputs}
     * into a single {@link Output}{@literal <T>} with an {@link java.util.List}{@literal <T>}
     * containing all their underlying values.
     * <p>
     * If any of the {@link Output}{@literal <T>}s are not known, the final result will be not known.
     * Similarly, if any of the {@link Output}{@literal <T>}s are secrets, then the final result will be a secret.
     *
     * @param <T>     the type of the value
     * @param outputs the outputs to be combined
     * @return an {@link Output} with a list of all values of the given {@code outputs}
     */
    @SafeVarargs // safe because we only call List.of, that is also @SafeVarargs
    static <T> Output<List<T>> all(Output<T>... outputs) {
        return all(List.of(outputs));
    }

    /**
     * @param <T>     the type of the value
     * @param outputs the outputs to be combined
     * @return an {@link Output} with a list of all values of the given {@code outputs}
     * @see Output#all(Output[])  for more details.
     */
    static <T> Output<List<T>> all(Iterable<Output<T>> outputs) {
        return all(newArrayList(outputs));
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
     * Takes in a {@code formattableString} with potential {@link Output} or a regular {@link Object}.
     * in the 'placeholder holes'.
     * Conceptually, this method unwraps all the underlying values in the holes,
     * combines them appropriately with the {@code formattableString}, and produces an {@link Output}
     * containing the final result.
     * <p>
     * If any of the {@link Output}s are not known, the
     * final result will be not known.
     * <p>
     * Similarly, if any of the {@link Output}s are secrets,
     * then the final result will be a secret.
     *
     * @param formattableString The format string with same syntax as expected by {@link String#format(String, Object...)},
     *                          the behaviour on a {@code null} argument depends on the format conversion.
     * @param arguments         {@link Output}s or regular {@link Object}s that values of will be applied to the format String
     * @return an {@link Output} with the result of the formatting
     */
    static Output<String> format(String formattableString, @Nullable Object... arguments) {
        var argumentsOrEmpty = arguments == null ? List.of() : newArrayList(arguments);
        var data = argumentsOrEmpty.stream()
                .map(Output::ensureOutput)
                .map(OutputData::copyInputOutputData)
                .collect(toCollection(ArrayList::new)); // ArrayList preserves null

        var futureResult = CompletableFutures.allOf(data)
                .thenApply(dataList ->
                        OutputData.builder(new ArrayList<>(dataList.size()))
                                .accumulate(dataList, (ts, t) -> {
                                    ts.add(t);
                                    return ts;
                                })
                                .build()
                )
                .thenApply(
                        objs -> objs.apply(v -> String.format(formattableString, v.toArray()))
                );
        return new OutputInternal<>(futureResult);
    }

    private static Output<Object> ensureOutput(@Nullable Object o) {
        if (o instanceof Output) {
            //noinspection unchecked
            return (Output<Object>) o;
        }
        return Output.ofNullable(o);
    }

    // Convenience methods for Either (a.k.a. Union)

    /**
     * Represents an {@link Output} value that can be one of two different types.
     * For example, it might potentially be an "Integer" some time
     * or a "String" in other cases.
     *
     * @param <L>   the type contained by the {@link Left} instance
     * @param <R>   the type contained by the {@link Right} instance
     * @param value the value to create the {@link Left} instance with
     * @return an {@link Output} holding {@link Left} instance with the given value
     */
    static <L, R> Output<Either<L, R>> ofLeft(L value) {
        return Output.of(Either.ofLeft(value));
    }

    /**
     * @param <L>   the type contained by the {@link Left} instance
     * @param <R>   the type contained by the {@link Right} instance
     * @param value the value to create the {@link Right} instance with
     * @return an {@link Output} holding {@link Right} instance with the given value
     * @see #ofLeft(Object)
     */
    static <L, R> Output<Either<L, R>> ofRight(R value) {
        return Output.of(Either.ofRight(value));
    }

    /**
     * @param <L>   the type contained by the {@link Left} instance
     * @param <R>   the type contained by the {@link Right} instance
     * @param value an {@link Output} with the value to create the {@link Left} instance with
     * @return an {@link Output} holding {@link Left} instance with the value of the given {@link Output}
     * @see #ofLeft(Object)
     */
    static <L, R> Output<Either<L, R>> ofLeft(Output<L> value) {
        return new OutputInternal<>(Internal.of(value).getDataAsync()
                .thenApply(ioData -> ioData.apply(Either::<L, R>ofLeft)));
    }

    /**
     * @param <L>   the type contained by the {@link Left} instance
     * @param <R>   the type contained by the {@link Right} instance
     * @param value an {@link Output} with the value to create the {@link Right} instance with
     * @return an {@link Output} holding {@link Right} instance with the value of the given {@link Output}
     * @see #ofLeft(Object)
     */
    static <L, R> Output<Either<L, R>> ofRight(Output<R> value) {
        return new OutputInternal<>(Internal.of(value).getDataAsync()
                .thenApply(ioData -> ioData.apply(Either::ofRight)));
    }

    // Convenience methods for JSON

    /**
     * @return a {@link JsonNull#INSTANCE}
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> ofJson() {
        return ofJson(JsonNull.INSTANCE);
    }

    /**
     * Represents an {@link Output} value that wraps a {@link JsonElement}
     *
     * @param json the {@link JsonElement} to wrap
     * @return given {@link JsonElement} wrapped in an {@link Output}
     */
    static Output<JsonElement> ofJson(JsonElement json) {
        return Output.of(json);
    }

    /**
     * @param json the json value to wrap
     * @return given json value as a {@link JsonElement} wrapped in an {@link Output}
     * @throws com.google.gson.JsonSyntaxException – if json is not valid
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> parseJson(String json) {
        var gson = new Gson();
        return ofJson(gson.fromJson(json, JsonElement.class));
    }

    /**
     * @param json the json value wrapped in an {@link Output}{@literal <String>}
     * @return given json value as a {@link JsonElement} wrapped in an {@link Output}
     * @throws com.google.gson.JsonSyntaxException – if json is not valid
     * @see #ofJson(JsonElement)
     */
    static Output<JsonElement> parseJson(Output<String> json) {
        var gson = new Gson();
        return json.applyValue((String j) -> gson.fromJson(j, JsonElement.class));
    }

    // Convenience methods for List

    /**
     * Returns a shallow copy of the {@link List} wrapped in an {@link Output}
     *
     * @return an {@link Output} holding a copy of the given list
     */
    static <E> Output<List<E>> copyOfList(List<E> values) {
        return Output.of(ImmutableList.copyOf(values));
    }

    /**
     * Concatenates two lists of {@link Output}, can take a {@code null} value that will be treated as an empty list,
     * always returns {@code non-null}.
     *
     * @param <E>   type of the list element
     * @param left  fist list to concatenate
     * @param right second list to concatenate
     * @return an {@link Output} with {@code left} and {@code right} lists concatenated
     */
    @Nonnull
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
     * @return an {@link Output} with an empty {@link List}
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList() {
        return Output.of(ImmutableList.of());
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with one element.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1) {
        return Output.of(ImmutableList.of(e1));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with two elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2) {
        return Output.of(ImmutableList.of(e1, e2));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with three elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3) {
        return Output.of(ImmutableList.of(e1, e2, e3));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with four elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with five elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with six elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with seven elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with eight elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with nine elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with ten elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with eleven elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with twelve elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    static <E> Output<List<E>> ofList(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12));
    }

    /**
     * @return an {@link Output} value that wraps a {@link List} with more than twelve elements.
     * @see #ofList(Object)
     * @see #listBuilder()
     */
    @SafeVarargs
    static <E> Output<List<E>> ofList(
            E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
        return Output.of(ImmutableList.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, others));
    }

    /**
     * Helps to build an {@link Output} that wraps a {@link List}.
     *
     * @return an {@link Output.ListBuilder}{@literal <E>} instance
     * @see #ofList(Object)
     */
    static <E> Output.ListBuilder<E> listBuilder() {
        return new Output.ListBuilder<>();
    }

    /**
     * A {@link List} wrapped in an {@link Output} builder.
     */
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
     * Returns a shallow copy of the {@link Map} wrapped in an {@link Output}
     *
     * @return an {@link Output} holding a copy of the given map
     */
    static <V> Output<Map<String, V>> copyOfMap(Map<String, V> values) {
        return Output.of(ImmutableMap.copyOf(values));
    }

    /**
     * Concatenates two {@link Map} wrapped in an {@link Output}.
     * Returns a new instance without modifying any of the arguments.
     * <p>
     * If both maps contain the same key, the value from the second map takes over.
     * <p>
     * {@code null} values in the Output or Map layer are treated as empty maps.
     *
     * @param <V>   type of the map value
     * @param left  The first @see {@code Output<Map<String, V>>} to concatenate
     * @param right The second @see {@code Output<Map<String, V>>} to concatenate, it has higher priority in case of key clash
     * @return an {@code Output<Map<String, V>>} that contains the items from both maps given
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
     * @return an {@link Output} with an empty {@link Map}
     * @see #ofMap(String, Object)
     * @see #mapBuilder()
     */
    static <V> Output<Map<String, V>> ofMap() {
        return Output.of(ImmutableMap.of());
    }

    /**
     * @return an {@link Output} that wraps a {@link Map} with one pair.
     * @see #mapBuilder()
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1) {
        return Output.of(ImmutableMap.of(key1, value1));
    }

    /**
     * @return an {@link Output} that wraps a {@link Map} with two pairs.
     * @see #ofMap(String, Object)
     * @see #mapBuilder()
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                            String key2, V value2) {
        return Output.of(ImmutableMap.of(key1, value1, key2, value2));
    }

    /**
     * @return an {@link Output} that wraps a {@link Map} with three pairs.
     * @see #ofMap(String, Object)
     * @see #mapBuilder()
     */
    static <V> Output<Map<String, V>> ofMap(String key1, V value1,
                                            String key2, V value2,
                                            String key3, V value3) {
        return Output.of(ImmutableMap.of(key1, value1, key2, value2, key3, value3));
    }

    /**
     * @return an {@link Output} that wraps a {@link Map} with four pairs.
     * @see #ofMap(String, Object)
     * @see #mapBuilder()
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
     * @return an {@link Output} that wraps a {@link Map} with five pairs.
     * @see #ofMap(String, Object)
     * @see #mapBuilder()
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
     * Helps to build a {@link Map} wrapped in an {@link Output}.
     *
     * @see #ofMap(String, Object)
     */
    static <E> Output.MapBuilder<E> mapBuilder() {
        return new Output.MapBuilder<>();
    }

    /**
     * A {@link Map} wrapped in an {@link Output} builder.
     */
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
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @return an {@link Output} holding a {@link Tuple2} with all given items
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2> Output<Tuple2<T1, T2>> tuple(Output<T1> item1, Output<T2> item2) {
        return tuple(item1, item2, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2));
    }

    /**
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param <T3>  type of {@code item3}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @param item3 the 3rd item ot the tuple
     * @return an {@link Output} holding a {@link Tuple3} with all given items
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3> Output<Tuple3<T1, T2, T3>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3
    ) {
        return tuple(item1, item2, item3, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3));
    }

    /**
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param <T3>  type of {@code item3}
     * @param <T4>  type of {@code item4}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @param item3 the 3rd item ot the tuple
     * @param item4 the 4th item ot the tuple
     * @return an {@link Output} holding a {@link Tuple4} with all given items
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4> Output<Tuple4<T1, T2, T3, T4>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4
    ) {
        return tuple(item1, item2, item3, item4, TupleZeroOut, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4));
    }

    /**
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param <T3>  type of {@code item3}
     * @param <T4>  type of {@code item4}
     * @param <T5>  type of {@code item5}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @param item3 the 3rd item ot the tuple
     * @param item4 the 4th item ot the tuple
     * @param item5 the 5th item ot the tuple
     * @return an {@link Output} holding a {@link Tuple5} with all given items
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5> Output<Tuple5<T1, T2, T3, T4, T5>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5
    ) {
        return tuple(item1, item2, item3, item4, item5, TupleZeroOut, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5));
    }

    /**
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param <T3>  type of {@code item3}
     * @param <T4>  type of {@code item4}
     * @param <T5>  type of {@code item5}
     * @param <T6>  type of {@code item6}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @param item3 the 3rd item ot the tuple
     * @param item4 the 4th item ot the tuple
     * @param item5 the 5th item ot the tuple
     * @param item6 the 6th item ot the tuple
     * @return an {@link Output} holding a {@link Tuple6} with all given items
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5, T6> Output<Tuple6<T1, T2, T3, T4, T5, T6>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, TupleZeroOut, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6));
    }

    /**
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param <T3>  type of {@code item3}
     * @param <T4>  type of {@code item4}
     * @param <T5>  type of {@code item5}
     * @param <T6>  type of {@code item6}
     * @param <T7>  type of {@code item7}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @param item3 the 3rd item ot the tuple
     * @param item4 the 4th item ot the tuple
     * @param item5 the 5th item ot the tuple
     * @param item6 the 6th item ot the tuple
     * @param item7 the 7th item ot the tuple
     * @return an {@link Output} holding a {@link Tuple7} with all given items
     * @see Output#tuple(Output, Output, Output, Output, Output, Output, Output, Output)
     */
    static <T1, T2, T3, T4, T5, T6, T7> Output<Tuple7<T1, T2, T3, T4, T5, T6, T7>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7
    ) {
        return tuple(item1, item2, item3, item4, item5, item6, item7, TupleZeroOut)
                .applyValue(v -> Tuples.of(v.t1, v.t2, v.t3, v.t4, v.t5, v.t6, v.t7));
    }

    /**
     * Combines all the {@link Output} values in the provided parameters and combines
     * them all into a single tuple containing each of their underlying values.
     * If any of the {@link Output}s are not known, the final result will be not known.  Similarly,
     * if any of the {@link Output}s are secrets, then the final result will be a secret.
     *
     * @param <T1>  type of {@code item1}
     * @param <T2>  type of {@code item2}
     * @param <T3>  type of {@code item3}
     * @param <T4>  type of {@code item4}
     * @param <T5>  type of {@code item5}
     * @param <T6>  type of {@code item6}
     * @param <T7>  type of {@code item7}
     * @param <T8>  type of {@code item8}
     * @param item1 the 1st item ot the tuple
     * @param item2 the 2nd item ot the tuple
     * @param item3 the 3rd item ot the tuple
     * @param item4 the 4th item ot the tuple
     * @param item5 the 5th item ot the tuple
     * @param item6 the 6th item ot the tuple
     * @param item7 the 7th item ot the tuple
     * @param item8 the 8th item ot the tuple
     * @return an {@link Output} holding a {@link Tuple8} with all given items
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8> Output<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> tuple(
            Output<T1> item1, Output<T2> item2, Output<T3> item3, Output<T4> item4,
            Output<T5> item5, Output<T6> item6, Output<T7> item7, Output<T8> item8
    ) {
        return new OutputInternal<>(OutputData.tuple(item1, item2, item3, item4, item5, item6, item7, item8));
    }
}
