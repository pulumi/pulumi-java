package com.pulumi.core;

import com.google.common.collect.ImmutableSet;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OutputTests {

    public static <T> Output<T> unknown() {
        return new OutputInternal<>(OutputData.ofNullable(ImmutableSet.of(), null, false, false));
    }

    public static <T> Output<T> unknownSecret() {
        return new OutputInternal<>(OutputData.ofNullable(ImmutableSet.of(), null, false, true));
    }

    @Test
    void testOutputToStringMessage() {
        var o = Output.of(0);
        assertThat(o.toString()).endsWith("This function may throw in a future version of Pulumi.");
    }

    @Test
    @SetEnvironmentVariable(key = "PULUMI_ERROR_OUTPUT_STRING", value = "true")
    void testOutputToStringThrowsWhenEnvVarSetTrue() {
        var o = Output.of(0);
        var exception = assertThrows(IllegalStateException.class, () -> {
            o.toString();
        });
        assertThat(exception.getMessage()).endsWith(
                "See https://www.pulumi.com/docs/concepts/inputs-outputs for more details.");
    }

    @Test
    @SetEnvironmentVariable(key = "PULUMI_ERROR_OUTPUT_STRING", value = "1")
    void testOutputToStringThrowsWhenEnvVarSet1() {
        var o = Output.of(0);
        var exception = assertThrows(IllegalStateException.class, () -> {
            o.toString();
        });
        assertThat(exception.getMessage()).endsWith(
                "See https://www.pulumi.com/docs/concepts/inputs-outputs for more details.");
    }
}
