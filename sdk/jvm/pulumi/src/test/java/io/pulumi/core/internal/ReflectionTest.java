package io.pulumi.core.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.pulumi.core.internal.Reflection.isAssignablePrimitiveFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ReflectionTest {

    @SuppressWarnings("unused")
    private static Stream<Arguments> testIsAssignablePrimitiveFrom() {
        return Stream.of(
                arguments(boolean.class, Boolean.class, true),
                arguments(byte.class, Byte.class, true),
                arguments(char.class, Character.class, true),
                arguments(double.class, Double.class, true),
                arguments(float.class, Float.class, true),
                arguments(int.class, Integer.class, true),
                arguments(long.class, Long.class, true),
                arguments(short.class, Short.class, true),
                arguments(Boolean.class, boolean.class, true),
                arguments(Byte.class, byte.class, true),
                arguments(Character.class, char.class, true),
                arguments(Double.class, double.class, true),
                arguments(Float.class, float.class, true),
                arguments(Integer.class, int.class, true),
                arguments(Long.class, long.class, true),
                arguments(Short.class, short.class, true),
                arguments(Boolean.class, Boolean.class, true),
                arguments(boolean.class, boolean.class, true),
                arguments(byte.class, boolean.class, false),
                arguments(Byte.class, Boolean.class, false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIsAssignablePrimitiveFrom(Class<?> to, Class<?> from, boolean expected) {
        assertThat(isAssignablePrimitiveFrom(to, from)).isEqualTo(expected);
    }

}