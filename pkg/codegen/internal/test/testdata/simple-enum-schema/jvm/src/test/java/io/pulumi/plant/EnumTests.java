package io.pulumi.plant;

import org.junit.jupiter.api.Test;

import io.pulumi.plant.enums.*;

import static org.assertj.core.api.Assertions.assertThat;

class EnumTests {

    @Test
    void testEnumContainerBrightness() {
        assertThat(ContainerBrightness.ZeroPointOne.getValue()).isEqualTo(0.1);
        assertThat(ContainerBrightness.One.getValue()).isEqualTo(1.0);
    }
}