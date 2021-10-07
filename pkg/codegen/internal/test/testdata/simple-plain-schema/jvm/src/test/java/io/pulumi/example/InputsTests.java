package io.pulumi.example;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InputTests {
    @Test
    void testComplexResourceArgs1_nullValues() {
        var args = new Inputs.FooArgs();
        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("a");
        assertThat(map).containsKey("b");
        assertThat(map).containsKey("c");
        assertThat(map).containsKey("d");
        assertThat(map).containsKey("e");
        assertThat(map).containsKey("f");

        assertThat(map).containsEntry("a", Optional.empty());
        assertThat(map).containsEntry("b", Optional.empty());
        assertThat(map).containsEntry("c", Optional.empty());
        assertThat(map).containsEntry("d", Optional.empty());
        assertThat(map).containsEntry("e", Optional.empty());
        assertThat(map).containsEntry("f", Optional.empty());
    }
}