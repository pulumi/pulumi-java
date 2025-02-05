// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import org.junit.jupiter.api.Test;

import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectSettingsTest {
    @Test
    void testDefault() {
        var d = ProjectSettings.createDefault("foo");
        assertThat(d.equals(d)).isTrue();
        assertThat(d.isDefault()).isTrue();

        var other = ProjectSettings.builder("bar", ProjectRuntimeName.DOTNET).build();
        assertThat(d.equals(other)).isFalse();
        assertThat(other.equals(d)).isFalse();
        assertThat(other.isDefault()).isFalse();
    }

    @Test
    void testSerializeToYaml() {
        var d = ProjectSettings.createDefault("foo");

        var serializer = new LocalSerializer();
        var result = serializer.serializeYaml(d);
        assertThat(result).isEqualTo("name: foo\nruntime: java\n");

        var deserialized = serializer.deserializeYaml(result, ProjectSettings.class);
        assertThat(d.equals(deserialized)).isTrue();
        assertThat(deserialized.equals(d)).isTrue();
    }
}
