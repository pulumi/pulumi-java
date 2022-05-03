package io.pulumi.resources;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

import static io.pulumi.resources.Resources.copyNullableList;
import static io.pulumi.resources.Resources.mergeNullableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ResourcesTest {

    @SuppressWarnings("unused")
    private static Stream<Arguments> testMergeNullableList() {
        return Stream.of(
                arguments(null, null, null),
                arguments(null, List.of(), List.of()),
                arguments(List.of(), null, List.of()),
                arguments(List.of(), List.of(), List.of()),
                arguments(List.of("a"), List.of("b"), List.of("a", "b")),
                arguments(null, List.of("b", "c"), List.of("b", "c"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMergeNullableList(@Nullable List<String> left, @Nullable List<String> right, @Nullable List<String> expected) {
        assertEquals(expected, mergeNullableList(left, right));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testCopyNullableList() {
        return Stream.of(
                arguments(null, null),
                arguments(List.of(), List.of()),
                arguments(List.of("a"), List.of("a")),
                arguments(List.of("a", "b"), List.of("a", "b"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCopyNullableList(@Nullable List<String> list, @Nullable List<String> expected) {
        assertEquals(expected, copyNullableList(list));
    }

}