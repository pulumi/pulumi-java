package io.pulumi.core.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputOutputDataTest {

    @Test
    void testHashCodeEqualsContract() {
        assertThat(InputOutputData.empty()).isEqualTo(InputOutputData.empty());
        assertThat(InputOutputData.empty()).isNotEqualTo(InputOutputData.of(1));
    }
}