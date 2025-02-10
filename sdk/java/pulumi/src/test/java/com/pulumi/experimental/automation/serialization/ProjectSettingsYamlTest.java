// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.pulumi.experimental.automation.*;
import com.pulumi.experimental.automation.serialization.internal.LocalSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectSettingsYamlTest {
    private static final LocalSerializer serializer = new LocalSerializer();

    @ParameterizedTest
    @CsvSource({
            "'', 'Missing name'",
            "'name: test-project', 'Missing runtime'",
            "'name: test-project\nruntime:\n  options:\n    typescript: true', 'Missing runtime name'",
            "'runtime: java', 'Missing name'",
    })
    void testDeserializeMissingRequiredValuesThrows(String yaml, String message) {
        var exception = assertThrows(NullPointerException.class, () -> {
            serializer.deserializeYaml(yaml, ProjectSettings.class);
        });
        assertThat(exception.getMessage()).isEqualTo(message);
    }

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
        assertThat(settings.getRuntime().name()).isEqualTo(runtime);
        assertThat(settings.getRuntime().options()).isNull();

        // Test roundtrip
        var serialized = serializer.serializeYaml(settings);
        assertThat(serialized).isEqualTo(yaml);
    }

    @ParameterizedTest
    @EnumSource(ProjectRuntimeName.class)
    void testDeserializeWithObjectRuntime(ProjectRuntimeName runtime) {
        var yaml = String.format("name: test-project\n" +
                "runtime:\n" +
                "  name: %s\n" +
                "  options:\n" +
                "    binary: test-binary\n" +
                "    typescript: false\n" +
                "    virtualenv: test-env\n",
                runtime.toString().toLowerCase());

        var settings = serializer.deserializeYaml(yaml, ProjectSettings.class);

        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(ProjectSettings.class);
        assertThat(settings.getName()).isEqualTo("test-project");
        assertThat(settings.getRuntime().name()).isEqualTo(runtime);
        var options = settings.getRuntime().options();
        assertThat(options).isNotNull();
        assertThat(options.binary()).isEqualTo("test-binary");
        assertThat(options.typescript()).isFalse();
        assertThat(options.virtualenv()).isEqualTo("test-env");

        // Test roundtrip
        var serialized = serializer.serializeYaml(settings);
        assertThat(serialized).isEqualTo(yaml);
    }

    @Test
    void testDeserializeTemplate() {
        var yaml = "name: test-project\n" +
                "runtime: java\n" +
                "template:\n" +
                "  config:\n" +
                "    foo:\n" +
                "      default: bar\n" +
                "      description: foo description\n" +
                "      secret: true\n" +
                "  description: some description\n" +
                "  displayName: my template\n" +
                "  quickstart: quickstart text\n";

        var settings = serializer.deserializeYaml(yaml, ProjectSettings.class);

        assertThat(settings).isNotNull();
        assertThat(settings).isInstanceOf(ProjectSettings.class);
        assertThat(settings.getName()).isEqualTo("test-project");
        assertThat(settings.getRuntime().name()).isEqualTo(ProjectRuntimeName.JAVA);
        assertThat(settings.getRuntime().options()).isNull();
        var template = settings.getTemplate();
        assertThat(template).isNotNull();
        var config = template.getConfig();
        assertThat(config).isNotNull();
        assertThat(config).containsKeys("foo");
        var foo = config.get("foo");
        assertThat(foo).isNotNull();
        assertThat(foo.getDefault()).isEqualTo("bar");
        assertThat(foo.getDescription()).isEqualTo("foo description");
        assertThat(foo.getSecret()).isTrue();
        assertThat(template.getDescription()).isEqualTo("some description");
        assertThat(template.getDisplayName()).isEqualTo("my template");
        assertThat(template.getQuickstart()).isEqualTo("quickstart text");

        // Test roundtrip
        var serialized = serializer.serializeYaml(settings);
        assertThat(serialized).isEqualTo(yaml);
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

    @Test
    void testSerializeFull() {
        var expected = "backend:\n" +
                "  url: url\n" +
                "config:\n" +
                "  bar:\n" +
                "    default: default\n" +
                "    description: bar desc\n" +
                "    items:\n" +
                "      items:\n" +
                "        type: string\n" +
                "      type: string\n" +
                "    secret: true\n" +
                "    value: value\n" +
                "name: test-project\n" +
                "options:\n" +
                "  refresh: always\n" +
                "plugins:\n" +
                "  providers:\n" +
                "  - name: name\n" +
                "    version: v1.0.0\n" +
                "runtime:\n" +
                "  name: java\n" +
                "  options:\n" +
                "    binary: binary/value\n" +
                "    typescript: false\n" +
                "    virtualenv: env\n" +
                "stackConfigDir: dir\n" +
                "template:\n" +
                "  config:\n" +
                "    foo:\n" +
                "      default: default\n" +
                "      description: my desc\n";

        var settings = ProjectSettings.builder("test-project", ProjectRuntime.builder(ProjectRuntimeName.JAVA)
                .options(ProjectRuntimeOptions.builder()
                        .typescript(false)
                        .binary("binary/value")
                        .virtualenv("env")
                        .build())
                .build())
                .config(Map.of("bar", ProjectConfigType.builder()
                        .description("bar desc")
                        .default_("default")
                        .value("value")
                        .secret(true)
                        .items(ProjectConfigItemsType.builder()
                                .type("string")
                                .items(ProjectConfigItemsType.builder()
                                        .type("string")
                                        .build())
                                .build())
                        .build()))
                .stackConfigDir("dir")
                .template(ProjectTemplate.builder()
                        .config(Map.of("foo", new ProjectTemplateConfigValue("my desc", "default")))
                        .build())
                .backend(ProjectBackend.builder()
                        .url("url")
                        .build())
                .options(ProjectOptions.builder()
                        .refresh("always")
                        .build())
                .plugins(ProjectPlugins.builder()
                        .providers(List.of(ProjectPluginOptions.builder()
                                .name("name")
                                .version("v1.0.0")
                                .build()))
                        .build())
                .build();

        var yaml = serializer.serializeYaml(settings);
        assertThat(yaml).isEqualTo(expected);

        var deserialize = serializer.deserializeYaml(yaml, ProjectSettings.class);
        assertThat(deserialize).isEqualTo(settings);
    }
}
