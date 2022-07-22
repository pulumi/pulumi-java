package com.pulumi.core;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TypeShapeTest {

    @Test
    void testTypeShapeToGSONConversion() {
        var token = TypeShape.list(Integer.class).toGSON().getType();
        var string = "[1,2,3,4]";

        var gson = new Gson();
        List<Integer> result = gson.fromJson(string, token);

        assertThat(result).isNotNull();
        assertThat(result).containsExactly(1, 2, 3, 4);
    }

    public static Stream<Arguments> testTypeShapeFromString() {
        return Stream.of(
                arguments("", "java.lang.Void"),
                arguments("java.lang.String", "java.lang.String"),
                arguments("java.lang.Character$Subset", "java.lang.Character$Subset"),
                arguments("java.lang.Character.Subset", "java.lang.Character$Subset"), // check non-canonical nested class
                arguments("java.util.List<java.lang.String>", "java.util.List<java.lang.String>"),
                arguments("java.util.Map<java.util.List<java.lang.String>,java.lang.String>", "java.util.Map<java.util.List<java.lang.String>,java.lang.String>"),
                arguments("com.pulumi.core.Either<java.util.Map<java.lang.String,java.util.List<java.lang.String>>,java.util.Map<java.lang.String,java.util.List<java.lang.String>>>", "com.pulumi.core.Either<java.util.Map<java.lang.String,java.util.List<java.lang.String>>,java.util.Map<java.lang.String,java.util.List<java.lang.String>>>")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testTypeShapeFromString(String given, String expected) {
        var result = TypeShape.fromString(given);
        assertThat(result.asString()).isEqualTo(expected);
    }
}