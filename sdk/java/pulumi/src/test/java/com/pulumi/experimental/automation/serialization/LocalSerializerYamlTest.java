// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalSerializerYamlTest {
    private static final LocalSerializer serializer = new LocalSerializer();

    @Test
    void testDynamic() {
        var yaml = "one: 123\n" +
            "two: two\n" +
            "three: true\n" +
            "nested:\n" +
            "  test: test\n" +
            "  testtwo: 123\n";

        Map<String, Object> map = serializer.deserializeYaml(yaml);

        assertThat(map)
            .isNotNull()
            .isNotEmpty()
            .hasSize(4);
    }
}
