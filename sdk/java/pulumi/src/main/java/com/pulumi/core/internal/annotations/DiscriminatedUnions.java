package com.pulumi.core.internal.annotations;

import com.google.common.collect.ImmutableSortedMap;
import com.pulumi.core.Either;
import com.pulumi.core.annotations.DiscriminatedUnion;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the {@link DiscriminatedUnion} annotations that Pulumi codegen puts on a generated union
 * interface, and resolves a raw value to the concrete member its discriminator tag selects.
 */
@InternalUse
public final class DiscriminatedUnions {

    private DiscriminatedUnions() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * @return the discriminator annotation of {@code type}, if it is a generated union interface
     */
    public static Optional<DiscriminatedUnion> of(Class<?> type) {
        return Optional.ofNullable(type.getAnnotation(DiscriminatedUnion.class));
    }

    /**
     * @return the members of the union {@code type}, keyed by discriminator tag, in tag order
     */
    public static ImmutableSortedMap<String, Class<?>> casesOf(Class<?> type) {
        var cases = ImmutableSortedMap.<String, Class<?>>naturalOrder();
        for (var unionCase : type.getAnnotationsByType(DiscriminatedUnion.Case.class)) {
            cases.put(unionCase.tag(), unionCase.type());
        }
        return cases.build();
    }

    /**
     * Resolves the union member that {@code properties} selects.
     *
     * @return the concrete member type, or a message explaining why no member matches
     */
    public static Either<String, Class<?>> resolve(
            Class<?> type, DiscriminatedUnion union, @Nullable Object value
    ) {
        if (!(value instanceof Map)) {
            return Either.ofLeft(String.format(
                    "Expected a map with a '%s' discriminator property to convert to '%s', got: '%s'",
                    union.value(), type.getTypeName(),
                    value == null ? "null" : value.getClass().getTypeName()
            ));
        }

        var tag = ((Map<?, ?>) value).get(union.value());
        return resolveTag(type, union, tag instanceof String ? (String) tag : null);
    }

    /**
     * Resolves the union member that {@code tag} selects.
     *
     * @return the concrete member type, or a message explaining why no member matches
     */
    public static Either<String, Class<?>> resolveTag(
            Class<?> type, DiscriminatedUnion union, @Nullable String tag
    ) {
        var cases = casesOf(type);

        if (cases.isEmpty()) {
            return Either.ofLeft(String.format(
                    "Expected '%s' annotated with @%s to also carry at least one @%s.Case, found none",
                    type.getTypeName(),
                    DiscriminatedUnion.class.getSimpleName(),
                    DiscriminatedUnion.class.getSimpleName()
            ));
        }

        var expected = String.join(", ", cases.keySet());
        if (tag == null) {
            return Either.ofLeft(String.format(
                    "Missing discriminator property '%s' of '%s'; expected one of: %s",
                    union.value(), type.getTypeName(), expected
            ));
        }

        var member = cases.get(tag);
        if (member == null) {
            return Either.ofLeft(String.format(
                    "Unknown '%s' value '%s' of '%s'; expected one of: %s",
                    union.value(), tag, type.getTypeName(), expected
            ));
        }

        return Either.ofRight(member);
    }
}
