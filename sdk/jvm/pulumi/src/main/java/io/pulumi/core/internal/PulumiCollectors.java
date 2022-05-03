package io.pulumi.core.internal;

import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public class PulumiCollectors {

    private PulumiCollectors() {
        throw new UnsupportedOperationException("static class");
    }

    public static <T> Collector<T, ?, T> toSingleton() {
        return toSingleton(cause -> new IllegalArgumentException(String.format(
                "Expected a single element, got: %s", cause
        )));
    }

    public static <T> Collector<T, ?, T> toSingleton(Function<String, RuntimeException> exceptionSupplier) {
        return Collector.of(
                AtomicReference::new,
                (acc, e) -> {
                    if (!acc.compareAndSet(null, e)) {
                        throw exceptionSupplier.apply("multiple");
                    }
                },
                (acc1, acc2) -> {
                    if (acc1.get() == null) {
                        return acc2;
                    } else if (acc2.get() != null) {
                        throw exceptionSupplier.apply("multiple");
                    } else {
                        return acc1;
                    }
                },
                acc -> {
                    if (acc.get() == null) {
                        throw exceptionSupplier.apply("empty or null");
                    }
                    //noinspection unchecked
                    return (T) acc.get();
                },
                Collector.Characteristics.UNORDERED
        );
    }

    public static <T, A1, A2, R1, R2> Collector<T, ?, Tuple2<R1, R2>> toBoth(Collector<T, A1, R1> c1, Collector<T, A2, R2> c2) {
        return Collector.of(
                () -> Tuples.of(c1.supplier().get(), c2.supplier().get()),
                (acc, e) -> {
                    c1.accumulator().accept(acc.t1, e);
                    c2.accumulator().accept(acc.t2, e);
                },
                (acc1, acc2) -> Tuples.of(
                        c1.combiner().apply(acc1.t1, acc2.t1),
                        c2.combiner().apply(acc2.t2, acc2.t2)
                ),
                (acc) -> Tuples.of(
                        c1.finisher().apply(acc.t1),
                        c2.finisher().apply(acc.t2)
                )
        );
    }

    public static <T, K1, K2, T1, T2> Collector<T, ?, Tuple2<ImmutableMap<K1, T1>, ImmutableMap<K2, T2>>> toTupleOfMaps2(
            Function<? super T, ? extends K1> key1Function,
            Function<? super T, ? extends K2> key2Function,
            Function<? super T, ? extends T1> t1Function,
            Function<? super T, ? extends T2> t2Function
    ) {
        requireNonNull(key1Function);
        requireNonNull(key1Function);
        requireNonNull(t1Function);
        requireNonNull(t2Function);
        return toBoth(toImmutableMap(key1Function, t1Function), toImmutableMap(key2Function, t2Function));
    }
}
