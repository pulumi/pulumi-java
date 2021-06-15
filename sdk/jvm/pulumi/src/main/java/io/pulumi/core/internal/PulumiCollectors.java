package io.pulumi.core.internal;

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class PulumiCollectors {

    public static <T> Collector<? super T, ?, T> toSingleton() {
        return toSingleton(size -> new IllegalStateException(String.format(
                "Expected a single element, got: %d", size
        )));
    }

    public static <T> Collector<? super T, ?, T> toSingleton(Function<Integer, RuntimeException> exceptionSupplier) {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw exceptionSupplier.apply(list.size());
                    }
                    return list.get(0);
                }
        );
    }
}
