package com.pulumi.core.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RegexPatternTest {

    @SuppressWarnings("unused")
    private static Stream<Arguments> testNamedMatch() {
        return Stream.of(
                arguments(
                        "^(?<package>com/pulumi/.*)/version.txt$",
                        "",
                        Map.of("package", Optional.empty())),
                arguments(
                        "^(?<package>com/pulumi/.*)/version.txt$",
                        "com/pulumi/random/version.txt",
                        Map.of("package", Optional.of("com/pulumi/random"))
                ),
                arguments(
                        "^(?<package>com/pulumi/(?<name>.+))/plugin.json$",
                        "com/pulumi/random/plugin.json",
                        Map.of(
                                "package", Optional.of("com/pulumi/random"),
                                "name", Optional.of("random")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testNamedMatch(String pattern, String input, Map<String, Optional<String>> expected) {
        var matcher = RegexPattern.of(pattern).matcher(input);
        for (var e : expected.entrySet()) {
            var match = matcher.namedMatch(e.getKey());
            assertThat(match).isEqualTo(e.getValue());
        }
    }
}