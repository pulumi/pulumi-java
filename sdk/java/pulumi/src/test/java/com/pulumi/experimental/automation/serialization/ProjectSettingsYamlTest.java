// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.pulumi.experimental.automation.*;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectSettingsYamlTest {
    private static final LocalSerializer serializer = new LocalSerializer();

    @ParameterizedTest
    @EnumSource(ProjectRuntimeName.class)
    void testDeserializeWithStringRuntime(ProjectRuntimeName runtime) {
        var yaml = String.format("name: test-project\n" +
                "runtime: %s\n",
                runtime.toString().toLowerCase());

        var settings = serializer.deserializeYaml(yaml, ProjectSettings.class);

        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(ProjectSettings.class);
        assertThat(settings.getName()).isEqualTo("test-project");
        assertThat(settings.getRuntime().getName()).isEqualTo(runtime);
        assertThat(settings.getRuntime().getOptions()).isNull();
    }

    // TODO fix YAML deserialization
    @Disabled("Need to fix YAML deserialization")
    @ParameterizedTest
    @EnumSource(ProjectRuntimeName.class)
    void testDeserializeWithObjectRuntime(ProjectRuntimeName runtime) {
        var yaml = String.format("name: test-project\n" +
                "runtime:\n" +
                "  name: %s\n" +
                "  options:\n" +
                "    typescript: true\n" +
                "    binary: test-binary\n" +
                "    virtualenv: test-env\n",
                runtime.toString().toLowerCase());

        var settings = serializer.deserializeYaml(yaml, ProjectSettings.class);

        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(ProjectSettings.class);
        assertThat(settings.getName()).isEqualTo("test-project");
        assertThat(settings.getRuntime().getName()).isEqualTo(runtime);
        var options = settings.getRuntime().getOptions();
        assertThat(options).isNotNull();
        assertThat(options.getTypescript()).isTrue();
        assertThat(options.getBinary()).isEqualTo("test-binary");
        assertThat(options.getVirtualEnv()).isEqualTo("test-env");
    }

    @ParameterizedTest
    @EnumSource(ProjectRuntimeName.class)
    void testSerializeAsStringIfOptionsIsNull(ProjectRuntimeName runtime) {
        var expected = String.format("name: test-project\n" +
                "runtime: %s\n",
                runtime.toString().toLowerCase());

        var settings = ProjectSettings.builder("test-project", runtime).build();
        var yaml = serializer.serializeYaml(settings);
        assertThat(yaml).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(ProjectRuntimeName.class)
    void testSerializeAsObjectIfOptionsIsNotNull(ProjectRuntimeName runtime) {
        var expected = String.format("name: test-project\n" +
                "runtime:\n" +
                "  name: %s\n" +
                "  options:\n" +
                "    typescript: true\n",
                runtime.toString().toLowerCase());
        var settings = ProjectSettings.builder("test-project",
                ProjectRuntime.builder(runtime)
                        .options(ProjectRuntimeOptions.builder()
                                .typescript(true)
                                .build())
                        .build())
                .build();
        var yaml = serializer.serializeYaml(settings);
        assertThat(yaml).isEqualTo(expected);
    }
}
