package io.pulumi.resources;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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

}