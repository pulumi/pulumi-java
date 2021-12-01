package io.pulumi.example;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import io.pulumi.example.inputs.*;

import static org.assertj.core.api.Assertions.assertThat;

class InputTests {

    @Test
    void testInputsFooArgs_nullValues() {
        var args = FooArgs.Empty;
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

    @Test
    void testInputsFooArgs_simpleValues() {
        var args = FooArgs.builder()
                .setA(true)
                .setB(true)
                .setC(1)
                .setD(2)
                .setE("test1")
                .setF("test2")
                .build();

        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("a");
        assertThat(map).containsKey("b");
        assertThat(map).containsKey("c");
        assertThat(map).containsKey("d");
        assertThat(map).containsKey("e");
        assertThat(map).containsKey("f");

        assertThat(map).containsEntry("a", Optional.of(true));
        assertThat(map).containsEntry("b", Optional.of(true));
        assertThat(map).containsEntry("c", Optional.of(1));
        assertThat(map).containsEntry("d", Optional.of(2));
        assertThat(map).containsEntry("e", Optional.of("test1"));
        assertThat(map).containsEntry("f", Optional.of("test2"));
    }
}