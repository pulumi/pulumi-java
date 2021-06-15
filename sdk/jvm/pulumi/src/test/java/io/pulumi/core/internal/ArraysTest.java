package io.pulumi.core.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class ArraysTest {

    @Test
    void testConcat() {
        final String[] trueValues = {"1"};
        final String[] falseValues = {"0"};

        assertThat(Arrays.concat(trueValues, falseValues)).containsExactly("1", "0");
    }

    @ParameterizedTest
    @CsvSource({
            "null, false, false",
            ", false, false",
            "true, true, false",
            "false, false, true",
    })
    void testContains(@Nullable String value, boolean expectedTrue, boolean expectedFalse) {
        final String[] trueValues = {"1", "t", "T", "true", "TRUE", "True"};
        final String[] falseValues = {"0", "f", "F", "false", "FALSE", "False"};

        assertThat(Arrays.contains(trueValues, value, String::equalsIgnoreCase)).isEqualTo(expectedTrue);
        assertThat(Arrays.contains(falseValues, value, String::equalsIgnoreCase)).isEqualTo(expectedFalse);
    }
}