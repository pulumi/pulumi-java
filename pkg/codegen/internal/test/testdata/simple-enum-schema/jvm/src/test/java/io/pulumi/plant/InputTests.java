package io.pulumi.plant;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import io.pulumi.core.Input;
import io.pulumi.plant.inputs.*;
import io.pulumi.plant.enums.*;

import static org.assertj.core.api.Assertions.assertThat;

class InputTests {

    @Test
    void testContainerArgs_nullValues() {
        var args = ContainerArgs.Empty;
        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("brightness");
        assertThat(map).containsKey("color");
        assertThat(map).containsKey("material");
        assertThat(map).containsKey("size");

        assertThat(map).containsEntry("brightness", Optional.empty());
        assertThat(map).containsEntry("color", Optional.empty());
        assertThat(map).containsEntry("material", Optional.empty());
        assertThat(map).containsEntry("size", Optional.empty());
    }

    @Test
    void testContainerArgs_simpleValues() {
        var args = ContainerArgs.builder()
                .setBrightness(Input.of(ContainerBrightness.ZeroPointOne))
                .setColor(Input.ofUnion(ContainerColor.Red, ContainerColor.class, String.class))
                .setMaterial(Input.of("glass"))
                .setSize(Input.of(ContainerSize.FourInch))
                .build();

        var map = args.internalTypedOptionalToMapAsync().join();

        assertThat(map).containsKey("brightness");
        assertThat(map).containsKey("color");
        assertThat(map).containsKey("material");
        assertThat(map).containsKey("size");

        assertThat(map).containsEntry("brightness", Optional.of(0.1));
        assertThat(map).containsEntry("color", Optional.of("red"));
        assertThat(map).containsEntry("material", Optional.of("glass"));
        assertThat(map).containsEntry("size", Optional.of(4));
    }
}