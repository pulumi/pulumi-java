package io.pulumi.core.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RegexPatternTest {

    @SuppressWarnings("unused")
    private static Stream<Arguments> testNamedMatch() {
        return Stream.of(
                arguments("^(?<package>io/pulumi/.*)/version.txt$", "", "package", Optional.empty()),
                arguments(
                        "^(?<package>io/pulumi/.*)/version.txt$",
                        "io/pulumi/random/version.txt",
                        "package",
                        Optional.of("io/pulumi/random")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void testNamedMatch(String pattern, String input, String name, Optional<String> expected) {
        var matcher = RegexPattern.of(pattern).matcher(input);
        var match = matcher.namedMatch(name);
        assertThat(match).isEqualTo(expected);
    }
}