package com.pulumi.resources;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class CustomTimeoutsTest {

    @Test
    void testHashCodeEqualsContract() {
        EqualsVerifier.forClass(CustomTimeouts.class).verify();
    }

    @TestFactory
    Stream<DynamicNode> testGolangString() {
        return Stream.of(
                dynamicTest("empty", () ->
                        assertThat(CustomTimeouts.golangString(Optional.empty())).isEmpty()),
                dynamicTest("0s", () ->
                        assertThat(CustomTimeouts.golangString(Optional.of(Duration.ZERO))).isEqualTo("0ns")),
                dynamicTest("1s", () ->
                        assertThat(CustomTimeouts.golangString(Optional.of(Duration.ofSeconds(1)))).isEqualTo("1000000000ns"))
        );
    }

    @TestFactory
    Stream<DynamicNode> testParseTimeoutString() {
        return Stream.of(
            // Basic cases
            dynamicTest("null input", () ->
                    assertThat(CustomTimeouts.parseTimeoutString(null)).isNull()),
            dynamicTest("empty input", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("")).isNull()),
            dynamicTest("zero", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("0")).isEqualTo(Duration.ZERO)),

            // Single unit cases
            dynamicTest("zero seconds", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("0s")).isEqualTo(Duration.ZERO)),
            dynamicTest("nanoseconds", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100ns")).isEqualTo(Duration.ofNanos(100))),
            dynamicTest("microseconds", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100us")).isEqualTo(Duration.ofNanos(100_000))),
            dynamicTest("microseconds with µ", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100µs")).isEqualTo(Duration.ofNanos(100_000))),
            dynamicTest("milliseconds", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100ms")).isEqualTo(Duration.ofMillis(100))),
            dynamicTest("seconds", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100s")).isEqualTo(Duration.ofSeconds(100))),
            dynamicTest("minutes", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100m")).isEqualTo(Duration.ofMinutes(100))),
            dynamicTest("hours", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100h")).isEqualTo(Duration.ofHours(100))),
            dynamicTest("days", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("100d")).isEqualTo(Duration.ofDays(100))),

            // Decimal numbers
            dynamicTest("decimal seconds", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("1.5s")).isEqualTo(Duration.ofMillis(1500))),
            
            // Negative durations
            dynamicTest("negative duration", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("-100ms")).isEqualTo(Duration.ofMillis(-100))),
            dynamicTest("positive sign", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("+100ms")).isEqualTo(Duration.ofMillis(100))),

            // Combined durations
            dynamicTest("combined duration", () ->
                    assertThat(CustomTimeouts.parseTimeoutString("2h45m"))
                            .isEqualTo(Duration.ofHours(2).plus(Duration.ofMinutes(45))))
        );
    }

    @Test
    void testParseTimeoutStringInvalidInput() {
        // Missing unit
        assertThat(assertThrows(IllegalArgumentException.class, () ->
                CustomTimeouts.parseTimeoutString("100")))
                .hasMessageContaining("missing unit in duration");

        // Invalid unit
        assertThat(assertThrows(IllegalArgumentException.class, () ->
                CustomTimeouts.parseTimeoutString("100x")))
                .hasMessageContaining("invalid unit in duration");

        // Invalid number
        assertThrows(NumberFormatException.class, () ->
                CustomTimeouts.parseTimeoutString("abc"));
    }

}