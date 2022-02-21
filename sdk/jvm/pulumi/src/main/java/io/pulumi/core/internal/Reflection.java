package io.pulumi.core.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static java.util.Objects.requireNonNull;

public class Reflection {

    private Reflection() {
        throw new UnsupportedOperationException("static class");
    }

    public static ImmutableList<Field> allFields(Class<?> type) {
        requireNonNull(type);
        var fieldsArray = type.getDeclaredFields();
        var fields = ImmutableList.<Field>builder();
        fields.add(fieldsArray);
        if (type.getSuperclass() != null) {
            fields.addAll(allFields(type.getSuperclass()));
        }
        return fields.build();
    }

    public static boolean isNestedClass(Class<?> type) {
        requireNonNull(type);
        return type.isMemberClass() && !Modifier.isStatic(type.getModifiers());
    }

    /**
     * Adds check for wrapped primitives (boxing) in addition to what original {@link Class#isAssignableFrom(Class)} does.
     *
     * @return true if given types are assignable, including primitive-wrapper pairs
     * @throws NullPointerException if any parameter is null
     */
    public static boolean isAssignablePrimitiveFrom(Class<?> to, Class<?> from) {
        requireNonNull(to);
        requireNonNull(from);
        if (from.isPrimitive() && to.isPrimitive()) {
            return to == from;
        }
        if (to.isPrimitive()) {
            return primitiveWrappers.get(to) == from;
        }
        if (from.isPrimitive()) {
            return primitiveWrappers.get(from) == to;
        }
        return to.isAssignableFrom(from);
    }

    private static final ImmutableMap<Class<?>, Class<?>> primitiveWrappers = ImmutableMap.<Class<?>, Class<?>>builder()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(double.class, Double.class)
            .put(float.class, Float.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(short.class, Short.class)
            .build();
}
