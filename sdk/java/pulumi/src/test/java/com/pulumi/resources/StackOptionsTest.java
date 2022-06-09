package com.pulumi.resources;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class StackOptionsTest {

    public static Stream<Arguments> testMerge() {
        final ResourceTransformation transformation1 = args -> Optional.empty();
        final ResourceTransformation transformation2 = args -> Optional.empty();
        return Stream.of(
                arguments(
                        null,
                        null,
                        new StackOptions(List.of())
                ),
                arguments(
                        null,
                        new StackOptions(List.of()),
                        new StackOptions(List.of())
                ),
                arguments(
                        new StackOptions(List.of()),
                        null,
                        new StackOptions(List.of())
                ),
                arguments(
                        new StackOptions(List.of()),
                        new StackOptions(List.of()),
                        new StackOptions(List.of())
                ),
                arguments(
                        new StackOptions(List.of(transformation1)),
                        new StackOptions(List.of()),
                        new StackOptions(List.of(transformation1))
                ),
                arguments(
                        new StackOptions(List.of()),
                        new StackOptions(List.of(transformation1)),
                        new StackOptions(List.of(transformation1))
                ),
                arguments(
                        new StackOptions(List.of(transformation1)),
                        new StackOptions(List.of(transformation2)),
                        new StackOptions(List.of(transformation1, transformation2))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMerge(@Nullable StackOptions first, @Nullable StackOptions second, StackOptions expected) {
        var result = StackOptions.merge(first, second);

        assertThat(result.resourceTransformations()).containsExactlyElementsOf(expected.resourceTransformations());
    }
}