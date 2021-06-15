package io.pulumi.core;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.pulumi.core.internal.CompletableFutures;
import io.pulumi.core.internal.Copyable;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Spliterator.SIZED;

/**
 * A mapping of {@code String}s to values that can be passed in as the arguments to
 * a @see {@link io.pulumi.resources.Resource}.
 * The individual values are themselves @see {@link Input<V>}s.
 * <p/>
 *
 * @see InputMap<V> differs from a normal @see {@link Map} in that it is
 * itself an @see {@link Input<V>}. For example, a @see {@link io.pulumi.resources.Resource}
 * that accepts an @see {@link InputMap<V> may accept not just a map but an @see {@link Output}
 * of a map as well.
 * This is important for cases where the @see {@link Output}
 * map from some {@link io.pulumi.resources.Resource} needs to be passed
 * into another {@link io.pulumi.resources.Resource}.
 * Or for cases where creating the map invariably produces an {@link Output} because
 * its resultant value is dependent on other {@link Output}s.
 * <p/>
 * This benefit of @see {@link InputMap<V>} is also a limitation. Because it represents
 * a list of values that may eventually be created, there is no way to simply iterate over,
 * or access the elements of the map synchronously.
 * <p/>
 * InputMap is designed to be easily used in object and collection initializers.
 * For example, a resource that accepts a map of values can be written easily in this form:
 * <p/>
 * <code>
 * new SomeResource("name", new SomeResourceArgs(
 * InputMap.of(
 * key1, value1,
 * key2, value2,
 * key3, value3,
 * )
 * ));
 * </code>
 */
public class InputMap<V> extends
        InputImpl<Map<String, V>, Input<Map<String, V>>>
        implements Input<Map<String, V>> {

    protected InputMap() {
        super(ImmutableMap.of());
    }

    protected InputMap(Map<String, V> values) {
        super(values, false);
    }

    protected InputMap(Input<Map<String, V>> inputs) {
        super(TypedInputOutput.cast(inputs).internalGetDataAsync().copy());
    }

    protected InputMap(InputOutputData<Map<String, V>> data) {
        super(data);
    }

    protected InputMap(CompletableFuture<InputOutputData<Map<String, V>>> values) {
        super(values);
    }

    @Override
    protected InputMap<V> newInstance(CompletableFuture<InputOutputData<Map<String, V>>> dataFuture) {
        return new InputMap<>(dataFuture);
    }

    @Override
    public <U> Input<U> apply(Function<Map<String, V>, Input<U>> func) {
        return new InputDefault<>(InputOutputData.apply(
                dataFuture.thenApply(data -> data.apply(m -> m)),
                func.andThen(input -> TypedInputOutput.cast(input).internalGetDataAsync())
        ));
    }

    @Override
    public InputMap<V> copy() {
        return new InputMap<>(this.dataFuture.copy());
    }

    /**
     * Concatenates an instance of @see {@link InputMap<V>} into this instance.
     * Returns a new @see {@link InputMap<V>} without modifying any of the arguments.
     * <p/>
     * If both maps contain the same key, the value from the second map takes over.
     *
     * @param other The second @see {@link InputMap<V>}, it has higher priority in case of key clash.
     * @return A new instance of @see {@link InputMap<V>} that contains the items from both input maps.
     */
    public InputMap<V> concat(InputMap<V> other) {
        Objects.requireNonNull(other);

        return new InputMap<>(
                Input.tuple(this, other).applyValue(
                        t -> Stream
                                .concat(t.t1.entrySet().stream(), t.t2.entrySet().stream())
                                .collect(toImmutableMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (v1, v2) -> v2 // in case of duplicate, ignore the v1
                                ))
                )
        );
    }

    // Static section -----

    public static <V> InputMap<V> copyOf(Map<String, V> values) {
        return new InputMap<>(ImmutableMap.copyOf(values));
    }

    public static <V> InputMap<V> empty() {
        return new InputMap<>();
    }

    public static <V> InputMap<V> of() {
        return new InputMap<>(ImmutableMap.of());
    }

    public static <V> InputMap<V> of(String key1, V value1) {
        return new InputMap<>(ImmutableMap.of(key1, value1));
    }

    public static <V> InputMap<V> of(String key1, V value1,
                                     String key2, V value2) {
        return new InputMap<>(ImmutableMap.of(key1, value1, key2, value2));
    }

    public static <V> InputMap<V> of(String key1, V value1,
                                     String key2, V value2,
                                     String key3, V value3) {
        return new InputMap<>(ImmutableMap.of(key1, value1, key2, value2, key3, value3));
    }

    public static <V> InputMap<V> of(String key1, V value1,
                                     String key2, V value2,
                                     String key3, V value3,
                                     String key4, V value4) {
        return new InputMap<>(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4));
    }

    public static <V> InputMap<V> of(String key1, V value1,
                                     String key2, V value2,
                                     String key3, V value3,
                                     String key4, V value4,
                                     String key5, V value5) {
        return new InputMap<>(
                ImmutableMap.of(key1, value1, key2, value2,
                        key3, value3, key4, value4, key5, value5));
    }

    public static <V> InputMap.Builder<V> builder() {
        return new Builder<>();
    }

    public static final class Builder<V> {
        private final CompletableFutures.Builder<InputOutputData.Builder<ImmutableMap.Builder<String, V>>> builder;

        public Builder() {
            builder = CompletableFutures.builder(
                    CompletableFuture.completedFuture(InputOutputData.builder(ImmutableMap.builder()))
            );
        }

        @CanIgnoreReturnValue
        public <IO extends InputOutput<V, IO> & Copyable<IO>> InputMap.Builder<V> put(String key, InputOutput<V, IO> value) {
            this.builder.accumulate(
                    TypedInputOutput.cast(value).internalGetDataAsync(),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public InputMap.Builder<V> put(String key, V value) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(value)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data,
                            (mapBuilder, v) -> mapBuilder.put(key, v))
            );
            return this;
        }

        @CanIgnoreReturnValue
        public InputMap.Builder<V> put(Map.Entry<? extends String, ? extends V> entry) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(entry)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::put)
            );
            return this;
        }

        @CanIgnoreReturnValue
        public InputMap.Builder<V> putAll(Map<? extends String, ? extends V> map) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(map)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        @Beta
        @CanIgnoreReturnValue
        public InputMap.Builder<V> putAll(Iterable<? extends Map.Entry<? extends String, ? extends V>> entries) {
            this.builder.accumulate(
                    CompletableFuture.completedFuture(InputOutputData.of(entries)),
                    (dataBuilder, data) -> dataBuilder.accumulate(data, ImmutableMap.Builder::putAll)
            );
            return this;
        }

        public InputMap<V> build() {
            return new InputMap<>(builder.build(dataBuilder -> dataBuilder.build(ImmutableMap.Builder::build)));
        }
    }
}
