package com.pulumi.core;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
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

    public static Stream<Arguments> testTypeShapeFromTree() {
        return Stream.of(
                arguments("", new Class<?>[]{}, "java.lang.Void"),
                arguments("[]", new Class<?>[]{}, "java.lang.Void"),
                arguments("", new Class<?>[]{Integer.class}, "java.lang.Integer"),
                arguments("[0,1]", new Class<?>[]{List.class, Integer.class}, "java.util.List<java.lang.Integer>"),
                arguments(
                        "[0,1,2]", new Class<?>[]{Either.class, Integer.class, String.class},
                        "com.pulumi.core.Either<java.lang.Integer,java.lang.String>"
                ),
                arguments(
                        "[0,[0,[0,1]]]", new Class<?>[]{List.class, String.class},
                        "java.util.List<java.util.List<java.util.List<java.lang.String>>>"
                ),
                arguments(
                        "[0,[1,2,[3,2]],[1,[3,2],2]]",
                        new Class<?>[]{Either.class, Map.class, String.class, List.class},
                        "com.pulumi.core.Either<java.util.Map<java.lang.String,java.util.List<java.lang.String>>,java.util.Map<java.util.List<java.lang.String>,java.lang.String>>"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testTypeShapeFromTree(String treeJson, Class<?>[] refs, String expected) {
        var result = TypeShape.fromTree(refs, treeJson);
        assertThat(result.asString()).isEqualTo(expected);
    }
}