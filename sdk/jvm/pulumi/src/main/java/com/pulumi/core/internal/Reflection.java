package com.pulumi.core.internal;

import com.google.common.collect.ImmutableList;
import com.pulumi.core.Either;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Reflection {

    private Reflection() {
        throw new UnsupportedOperationException("static class");
    }

    public static ImmutableList<Field> allFields(Class<?> type) {
        Objects.requireNonNull(type);
        var fieldsArray = type.getDeclaredFields();
        var fields = ImmutableList.<Field>builder();
        fields.add(fieldsArray);
        if (type.getSuperclass() != null) {
            fields.addAll(allFields(type.getSuperclass()));
        }
        return fields.build();
    }

    public static boolean isNestedClass(Class<?> type) {
        Objects.requireNonNull(type);
        return type.isMemberClass() && !Modifier.isStatic(type.getModifiers());
    }

    private static <R> Either<IllegalArgumentException, R> enumUnderlyingType(Class<?> type, Function<Field, R> transformer) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(transformer);
        if (!type.isEnum()) {
            var ex = new IllegalArgumentException(String.format("Expected an Enum, got: '%s'", type.getTypeName()));
            ex.fillInStackTrace(); // pre-throw
            return Either.errorOf(ex);
        }
        var allFields = allFields(type);
        var fields = allFields.stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !field.getName().equals("ordinal"))
                .filter(field -> !field.getName().equals("name"))
                .collect(Collectors.toList());
        if (fields.size() == 0) {
            var field = allFields.stream()
                    .filter(f -> f.getName().equals("ordinal"))
                    .peek(f -> f.setAccessible(true))
                    .findFirst();
            if (field.isPresent()) {
                return Either.valueOf(transformer.apply(field.get()));
            }
            var ex = new IllegalArgumentException("Expected 'ordinal' filed, not found.");
            ex.fillInStackTrace(); // pre-throw
            return Either.errorOf(ex);
        }
        if (fields.size() == 1) {
            var field = fields.get(0);
            field.setAccessible(true);
            return Either.valueOf(transformer.apply(fields.get(0)));
        }

        var ex = new IllegalArgumentException(String.format(
                "Expected an Enum with zero or one custom fields, got %d in from '%s'",
                fields.size(), type.getTypeName()
        ));
        ex.fillInStackTrace(); // pre-throw
        return Either.errorOf(ex);
    }

}
