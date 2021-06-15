package io.pulumi.core.internal;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Arrays {

    private Arrays() {
        throw new UnsupportedOperationException("static class");
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] concat(T[] first, T[] second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        return Stream.concat(stream(first), stream(second))
                .toArray(
                        size -> (T[]) Array.newInstance(first.getClass().getComponentType(), size)
                );
    }

    public static <T> boolean contains(T[] array, @Nullable T value, BiFunction<T, /* @Nullable */ T, Boolean> comparator) {
        Objects.requireNonNull(array);
        Objects.requireNonNull(comparator);
        return stream(array)
                .map(v -> comparator.apply(v, value))
                .reduce(false, (b1, b2) -> b1 || b2);
    }
}
