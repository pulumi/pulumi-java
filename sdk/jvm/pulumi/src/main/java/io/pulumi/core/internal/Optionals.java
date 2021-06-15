package io.pulumi.core.internal;


import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class Optionals {

    private Optionals() {
        throw new UnsupportedOperationException("static class");
    }

    public static Optional<String> ofEmpty(@Nullable String value) {
        return ofEmpty(value, String::isEmpty);
    }

    public static Optional<String> ofBlank(@Nullable String value) {
        return ofEmpty(value, String::isBlank);
    }

    public static <T> Optional<T> ofEmpty(@Nullable T value, Predicate<T> isEmpty) {
        if (value == null) {
            return Optional.empty();
        }
        if (isEmpty.test(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
