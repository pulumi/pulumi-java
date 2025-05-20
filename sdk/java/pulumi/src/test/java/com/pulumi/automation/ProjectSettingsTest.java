// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import org.junit.jupiter.api.Test;

import com.pulumi.automation.ProjectRuntimeName;
import com.pulumi.automation.ProjectSettings;

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
}
