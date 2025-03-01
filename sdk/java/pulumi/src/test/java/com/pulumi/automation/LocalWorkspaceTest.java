// Copyright 2025, Pulumi Corporation

package com.pulumi.automation;

import com.pulumi.Context;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(LocalBackendExtension.class)
public class LocalWorkspaceTest {
    private static String normalizeConfigKey(String key, String projectName) {
        var parts = key.split(":");
        if (parts.length < 2)
            return projectName + ":" + key;

        return "";
    }

    private static String getTestSuffix() {
        var random = new Random();
        var result = random.nextInt(); // 31 bits, highest bit will be 0 (signed)
        return Integer.toHexString(result); // hex representation
    }

    private static String randomStackName() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }

    @ParameterizedTest
    @ValueSource(strings = { "yaml", "yml" })
    void testGetProjectSettings(String extension) throws Exception {
        Path workingDir = Paths.get(getClass().getResource("/" + extension).toURI());
        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder().workDir(workingDir).build())) {
            var result = workspace.getProjectSettings();
            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(settings -> {
                        assertThat(settings.name()).isEqualTo("testproj");
                        assertThat(settings.runtime().name()).isEqualTo(ProjectRuntimeName.GO);
                        assertThat(settings.description()).isEqualTo("A minimal Go Pulumi program");
                    });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "yaml", "yml" })
    void testGetStackSettings(String extension) throws Exception {
        Path workingDir = Paths.get(getClass().getResource("/" + extension).toURI());
        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder().workDir(workingDir).build())) {
            var result = workspace.getStackSettings("dev");
            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(settings -> {
                        assertThat(settings.secretsProvider()).isEqualTo("abc");
                        assertThat(settings.config()).isNotNull();

                        var config = settings.config();

                        assertThat(config).hasEntrySatisfying("plain", value -> {
                            assertThat(value.value()).isEqualTo("plain");
                            assertThat(value.isSecure()).isFalse();
                        });

                        assertThat(config).hasEntrySatisfying("secure", value -> {
                            assertThat(value.value()).isEqualTo("secret");
                            assertThat(value.isSecure()).isTrue();
                        });
                    });
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testAddRemoveListPlugins() throws Exception {
        var version = "6.70.0";
        var plugin = "aws";

        Predicate<PluginInfo> isAwsPlugin = p -> p.name().equals(plugin) && p.version().equals(version);

        var removeOptions = PluginRemoveOptions.builder()
                .name(plugin)
                .versionRange(version)
                .build();

        try (var workspace = LocalWorkspace.create()) {
            var plugins = workspace.listPlugins();
            if (plugins.stream().anyMatch(isAwsPlugin)) {
                workspace.removePlugin(removeOptions);
                plugins = workspace.listPlugins();
                assertThat(plugins).noneMatch(isAwsPlugin);
            }

            workspace.installPlugin(plugin, "v" + version);
            plugins = workspace.listPlugins();
            var aws = plugins.stream().filter(isAwsPlugin).findFirst().orElse(null);
            assertThat(aws).isNotNull();

            workspace.removePlugin(removeOptions);
            plugins = workspace.listPlugins();
            assertThat(plugins).noneMatch(isAwsPlugin);
        }
    }

    @Test
    void testCreateSelectRemoveStack(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .projectSettings(ProjectSettings.builder(
                        "create_select_remove_stack_test",
                        ProjectRuntimeName.NODEJS).build())
                .environmentVariables(env)
                .build())) {

            var stackName = randomStackName();

            Predicate<StackSummary> isStack = s -> s.name().equals(stackName);

            var stacks = workspace.listStacks();
            if (stacks.stream().anyMatch(isStack)) {
                workspace.removeStack(stackName);
                stacks = workspace.listStacks();
                assertThat(stacks).noneMatch(isStack);
            }

            workspace.createStack(stackName);
            stacks = workspace.listStacks();
            var newStack = stacks.stream().filter(isStack).findFirst();
            assertThat(newStack).isPresent().hasValueSatisfying(s -> {
                assertThat(s.isCurrent()).isTrue();
            });

            workspace.selectStack(stackName);
            workspace.removeStack(stackName);
            stacks = workspace.listStacks();
            assertThat(stacks).noneMatch(isStack);
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testImportExportStack(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        Path workingDir = Paths.get(getClass().getResource("/testproj").toURI());
        var projectSettings = ProjectSettings.builder("testproj", ProjectRuntimeName.GO)
                .description("A minimal Go Pulumi program")
                .build();

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .workDir(workingDir)
                .projectSettings(projectSettings)
                .environmentVariables(env)
                .build())) {

            var stackName = randomStackName();

            try {
                var stack = WorkspaceStack.create(stackName, workspace);

                var upResult = stack.up();
                assertThat(upResult.summary().kind()).isEqualTo(UpdateKind.UPDATE);
                assertThat(upResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                assertThat(upResult.outputs()).hasSize(3);

                var deployment = workspace.exportStack(stackName);
                assertThat(deployment.version()).isGreaterThan(0);

                var previewBeforeDestroy = stack.preview();
                assertThat(previewBeforeDestroy.changeSummary().get(OperationType.SAME)).isEqualTo(1);

                stack.destroy();

                var previewAfterDestroy = stack.preview();
                assertThat(previewAfterDestroy.changeSummary().get(OperationType.CREATE)).isEqualTo(1);

                workspace.importStack(stackName, deployment);

                // After we imported before-destroy deployment,
                // preview is back to reporting the before-destroy
                // state.

                var previewAfterImport = stack.preview();
                assertThat(previewAfterImport.changeSummary().get(OperationType.SAME)).isEqualTo(1);

                stack.destroy();
            } finally {
                workspace.removeStack(stackName);
            }
        }
    }

    @Test
    public void testManipulateConfig(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        var projectName = "manipulate_config_test";
        var projectSettings = ProjectSettings.builder(projectName, ProjectRuntimeName.NODEJS).build();

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .projectSettings(projectSettings)
                .environmentVariables(env)
                .build())) {

            var stackName = randomStackName();
            var stack = WorkspaceStack.create(stackName, workspace);

            var plainKey = normalizeConfigKey("plain", projectName);
            var secretKey = normalizeConfigKey("secret", projectName);

            try {
                assertThrows(AutomationException.class, () -> stack.getConfig(plainKey));

                var values = stack.getAllConfig();
                assertThat(values).isEmpty();

                var config = Map.of(
                        "plain", new ConfigValue("abc"),
                        "secret", new ConfigValue("def", true));
                stack.setAllConfig(config);

                values = stack.getAllConfig();

                var plainValue = values.get(plainKey);
                assertThat(plainValue).isNotNull();
                assertThat(plainValue.value()).isEqualTo("abc");
                assertThat(plainValue.isSecret()).isFalse();

                var secretValue = values.get(secretKey);
                assertThat(secretValue).isNotNull();
                assertThat(secretValue.value()).isEqualTo("def");
                assertThat(secretValue.isSecret()).isTrue();

                // Get individual configuration values
                plainValue = stack.getConfig(plainKey);
                assertThat(plainValue.value()).isEqualTo("abc");
                assertThat(plainValue.isSecret()).isFalse();

                secretValue = stack.getConfig(secretKey);
                assertThat(secretValue.value()).isEqualTo("def");
                assertThat(secretValue.isSecret()).isTrue();

                stack.removeConfig("plain");
                values = stack.getAllConfig();
                assertThat(values).hasSize(1);

                stack.setConfig("foo", new ConfigValue("bar"));
                values = stack.getAllConfig();
                assertThat(values).hasSize(2);
            } finally {
                workspace.removeStack(stackName);
            }
        }
    }

    @Test
    public void testManipulateConfigPath(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        var projectName = "manipulate_config_test";
        var projectSettings = ProjectSettings.builder(projectName, ProjectRuntimeName.NODEJS).build();

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .projectSettings(projectSettings)
                .environmentVariables(env)
                .build())) {

            var stackName = randomStackName();
            var stack = WorkspaceStack.create(stackName, workspace);

            try {
                // test backward compatibility
                stack.setConfig("key1", new ConfigValue("value1"));
                // test new flag without subPath
                stack.setConfig("key2", new ConfigValue("value2"), false);
                // test new flag with subPath
                stack.setConfig("key3.subKey1", new ConfigValue("value3"), true);
                // test secret
                stack.setConfig("key4", new ConfigValue("value4", true));
                // test subPath and key as secret
                stack.setConfig("key5.subKey1", new ConfigValue("value5", true), true);
                // test string with dots
                stack.setConfig("key6.subKey1", new ConfigValue("value6", true));
                // test string with dots
                stack.setConfig("key7.subKey1", new ConfigValue("value7", true), false);
                // test subPath
                stack.setConfig("key7.subKey2", new ConfigValue("value8"), true);
                // test subPath
                stack.setConfig("key7.subKey3", new ConfigValue("value9"), true);

                // test backward compatibility
                var cv1 = stack.getConfig("key1");
                assertThat(cv1).isNotNull();
                assertThat(cv1.value()).isEqualTo("value1");
                assertThat(cv1.isSecret()).isFalse();

                // test new flag without subPath
                var cv2 = stack.getConfig("key2", false);
                assertThat(cv2).isNotNull();
                assertThat(cv2.value()).isEqualTo("value2");
                assertThat(cv2.isSecret()).isFalse();

                // test new flag with subPath
                var cv3 = stack.getConfig("key3.subKey1", true);
                assertThat(cv3).isNotNull();
                assertThat(cv3.value()).isEqualTo("value3");
                assertThat(cv3.isSecret()).isFalse();

                // test secret
                var cv4 = stack.getConfig("key4");
                assertThat(cv4).isNotNull();
                assertThat(cv4.value()).isEqualTo("value4");
                assertThat(cv4.isSecret()).isTrue();

                // test subPath and key as secret
                var cv5 = stack.getConfig("key5.subKey1", true);
                assertThat(cv5).isNotNull();
                assertThat(cv5.value()).isEqualTo("value5");
                assertThat(cv5.isSecret()).isTrue();

                // test string with dots
                var cv6 = stack.getConfig("key6.subKey1");
                assertThat(cv6).isNotNull();
                assertThat(cv6.value()).isEqualTo("value6");
                assertThat(cv6.isSecret()).isTrue();

                // test string with dots
                var cv7 = stack.getConfig("key7.subKey1", false);
                assertThat(cv7).isNotNull();
                assertThat(cv7.value()).isEqualTo("value7");
                assertThat(cv7.isSecret()).isTrue();

                // test string with dots
                var cv8 = stack.getConfig("key7.subKey2", true);
                assertThat(cv8).isNotNull();
                assertThat(cv8.value()).isEqualTo("value8");
                assertThat(cv8.isSecret()).isFalse();

                // test string with dots
                var cv9 = stack.getConfig("key7.subKey3", true);
                assertThat(cv9).isNotNull();
                assertThat(cv9.value()).isEqualTo("value9");
                assertThat(cv9.isSecret()).isFalse();

                stack.removeConfig("key1");
                stack.removeConfig("key2", false);
                stack.removeConfig("key3", false);
                stack.removeConfig("key4", false);
                stack.removeConfig("key5", false);
                stack.removeConfig("key6.subKey1", false);
                stack.removeConfig("key7.subKey1", false);

                var cfg = stack.getAllConfig();
                assertThat(cfg.get(projectName + ":key7").value())
                        .isEqualTo("{\"subKey2\":\"value8\",\"subKey3\":\"value9\"}");
            } finally {
                workspace.removeStack(stackName);
            }
        }
    }

    @Test
    public void testSupportConfigFlagLike(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        var projectName = "config_flag_like";
        var projectSettings = ProjectSettings.builder(projectName, ProjectRuntimeName.NODEJS).build();

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .projectSettings(projectSettings)
                .environmentVariables(env)
                .build())) {

            var stackName = randomStackName();
            var stack = WorkspaceStack.create(stackName, workspace);

            var plainKey = normalizeConfigKey("key", projectName);
            var secretKey = normalizeConfigKey("secret-key", projectName);

            try {
                stack.setConfig("key", new ConfigValue("-value"));
                stack.setConfig("secret-key", new ConfigValue("-value", true));
                var values = stack.getAllConfig();
                var plainValue = values.get(plainKey);
                assertThat(plainValue).isNotNull();
                assertThat(plainValue.value()).isEqualTo("-value");
                assertThat(plainValue.isSecret()).isFalse();
                var secretValue = values.get(secretKey);
                assertThat(secretValue).isNotNull();
                assertThat(secretValue.value()).isEqualTo("-value");
                assertThat(secretValue.isSecret()).isTrue();
            } finally {
                workspace.removeStack(stackName);
            }
        }
    }

    @Test
    public void testListStackAndCurrentlySelected(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        var projectName = "node_list_test" + getTestSuffix();
        var projectSettings = ProjectSettings.builder(projectName, ProjectRuntimeName.NODEJS).build();

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .projectSettings(projectSettings)
                .environmentVariables(env)
                .build())) {

            var stackNames = new ArrayList<String>();
            try {
                for (var i = 0; i < 2; i++) {
                    var stackName = "int_test" + getTestSuffix();
                    WorkspaceStack.create(stackName, workspace);
                    stackNames.add(stackName);
                    var summary = workspace.getStack();
                    assertThat(summary).isPresent().hasValueSatisfying(s -> {
                        assertThat(s.isCurrent()).isTrue();
                    });
                    var stacks = workspace.listStacks();
                    assertThat(stacks).hasSize(i + 1);
                }
            } finally {
                for (var name : stackNames) {
                    workspace.removeStack(name);
                }
            }
        }
    }

    @Test
    public void testCheckStackStatus(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        var projectName = "check_stack_status_test";
        var projectSettings = ProjectSettings.builder(projectName, ProjectRuntimeName.NODEJS).build();

        try (var workspace = LocalWorkspace.create(LocalWorkspaceOptions.builder()
                .projectSettings(projectSettings)
                .environmentVariables(env)
                .build())) {

            var stackName = randomStackName();
            var stack = WorkspaceStack.create(stackName, workspace);
            try {
                var history = stack.getHistory();
                assertThat(history).isEmpty();
                var info = stack.getInfo();
                assertThat(info).isNotPresent();
            } finally {
                workspace.removeStack(stackName);
            }
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testStackLifecycleInlineProgram(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            Consumer<Context> program = ctx -> {
                var config = ctx.config();
                ctx.export("exp_static", "foo");
                ctx.export("exp_cfg", config.get("bar"));
                ctx.export("exp_secret", config.getSecret("buzz"));
            };

            var stackName = randomStackName();
            var projectName = "inline_java";
            try (var stack = LocalWorkspace.createStack(projectName, stackName, program,
                    LocalWorkspaceOptions.builder().environmentVariables(env).build())) {
                try {
                    var config = Map.of(
                            "bar", new ConfigValue("abc"),
                            "buzz", new ConfigValue("secret", true));
                    stack.setAllConfig(config);

                    // pulumi up
                    var upResult = stack.up();
                    assertThat(upResult.summary().kind()).isEqualTo(UpdateKind.UPDATE);
                    assertThat(upResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                    assertThat(upResult.outputs().size()).isEqualTo(3);

                    // exp_static
                    var expStaticValue = upResult.outputs().get("exp_static");
                    assertThat(expStaticValue.value()).isEqualTo("foo");
                    assertThat(expStaticValue.isSecret()).isFalse();

                    // exp_cfg
                    var expConfigValue = upResult.outputs().get("exp_cfg");
                    assertThat(expConfigValue.value()).isEqualTo("abc");
                    assertThat(expConfigValue.isSecret()).isFalse();

                    // exp_secret
                    var expSecretValue = upResult.outputs().get("exp_secret");
                    assertThat(expSecretValue.value()).isEqualTo("secret");
                    assertThat(expSecretValue.isSecret()).isTrue();

                    // pulumi preview
                    var previewResult = stack.preview();
                    var sameCount = previewResult.changeSummary().get(OperationType.SAME);
                    assertThat(sameCount).isEqualTo(1);

                    // pulumi refresh
                    var refreshResult = stack.refresh();
                    assertThat(refreshResult.summary().kind()).isEqualTo(UpdateKind.REFRESH);
                    assertThat(refreshResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);

                    // pulumi destroy
                    var destroyResult = stack.destroy();
                    assertThat(destroyResult.summary().kind()).isEqualTo(UpdateKind.DESTROY);
                    assertThat(destroyResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);

                } finally {
                    stack.workspace().removeStack(stackName);
                }
            }
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testInlineProgramDoesNotEmitWarning(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            Consumer<Context> program = ctx -> {
            };

            var stackName = randomStackName();
            var projectName = "inline_java";
            try (var stack = LocalWorkspace.createStack(projectName, stackName, program,
                    LocalWorkspaceOptions.builder().environmentVariables(env).build())) {
                try {

                    var stdout = new StringBuilder();
                    var stderr = new StringBuilder();
                    stack.preview(PreviewOptions.builder()
                            .onStandardOutput(stdout::append)
                            .onStandardError(stderr::append)
                            .build());

                    assertThat(stdout.toString()).doesNotContain("warning");
                    assertThat(stderr.toString()).doesNotContain("warning");
                } finally {
                    stack.workspace().removeStack(stackName);
                }
            }
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testStackLifecycleLocalProgram(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            var stackName = randomStackName();
            Path workingDir = Paths.get(getClass().getResource("/testproj").toURI());
            try (var stack = LocalWorkspace.createStack(stackName, workingDir,
                    LocalWorkspaceOptions.builder().environmentVariables(env).build())) {
                try {
                    var config = Map.of(
                            "bar", new ConfigValue("abc"),
                            "buzz", new ConfigValue("secret", true));
                    stack.setAllConfig(config);

                    // pulumi up
                    var upResult = stack.up();
                    assertThat(upResult.summary().kind()).isEqualTo(UpdateKind.UPDATE);
                    assertThat(upResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                    assertThat(upResult.outputs().size()).isEqualTo(3);

                    // exp_static
                    var expStaticValue = upResult.outputs().get("exp_static");
                    assertThat(expStaticValue.value()).isEqualTo("foo");
                    assertThat(expStaticValue.isSecret()).isFalse();

                    // exp_cfg
                    var expConfigValue = upResult.outputs().get("exp_cfg");
                    assertThat(expConfigValue.value()).isEqualTo("abc");
                    assertThat(expConfigValue.isSecret()).isFalse();

                    // exp_secret
                    var expSecretValue = upResult.outputs().get("exp_secret");
                    assertThat(expSecretValue.value()).isEqualTo("secret");
                    assertThat(expSecretValue.isSecret()).isTrue();

                    // pulumi preview
                    var previewResult = stack.preview();
                    var sameCount = previewResult.changeSummary().get(OperationType.SAME);
                    assertThat(sameCount).isEqualTo(1);

                    // pulumi refresh
                    var refreshResult = stack.refresh();
                    assertThat(refreshResult.summary().kind()).isEqualTo(UpdateKind.REFRESH);
                    assertThat(refreshResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);

                    // pulumi destroy
                    var destroyResult = stack.destroy();
                    assertThat(destroyResult.summary().kind()).isEqualTo(UpdateKind.DESTROY);
                    assertThat(destroyResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);

                } finally {
                    stack.workspace().removeStack(stackName);
                }
            }
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testSupportsStackOutputs(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            Consumer<Context> program = ctx -> {
                var config = ctx.config();
                ctx.export("exp_static", "foo");
                ctx.export("exp_cfg", config.get("bar"));
                ctx.export("exp_secret", config.getSecret("buzz"));
            };

            Consumer<Map<String, OutputValue>> assertOutputs = outputs -> {
                assertThat(outputs).hasSize(3);

                // exp_static
                var expStaticValue = outputs.get("exp_static");
                assertThat(expStaticValue.value()).isEqualTo("foo");
                assertThat(expStaticValue.isSecret()).isFalse();

                // exp_cfg
                var expConfigValue = outputs.get("exp_cfg");
                assertThat(expConfigValue.value()).isEqualTo("abc");
                assertThat(expConfigValue.isSecret()).isFalse();

                // exp_secret
                var expSecretValue = outputs.get("exp_secret");
                assertThat(expSecretValue.value()).isEqualTo("secret");
                assertThat(expSecretValue.isSecret()).isTrue();
            };

            var stackName = randomStackName();
            var projectName = "inline_java";
            try (var stack = LocalWorkspace.createStack(projectName, stackName, program,
                    LocalWorkspaceOptions.builder().environmentVariables(env).build())) {
                try {
                    var config = Map.of(
                            "bar", new ConfigValue("abc"),
                            "buzz", new ConfigValue("secret", true));
                    stack.setAllConfig(config);

                    var initialOutputs = stack.getOutputs();
                    assertThat(initialOutputs).isEmpty();

                    // pulumi up
                    var upResult = stack.up();
                    assertThat(upResult.summary().kind()).isEqualTo(UpdateKind.UPDATE);
                    assertThat(upResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                    assertOutputs.accept(upResult.outputs());

                    var outputsAfterUp = stack.getOutputs();
                    assertOutputs.accept(outputsAfterUp);

                    // pulumi destroy
                    var destroyResult = stack.destroy();
                    assertThat(destroyResult.summary().kind()).isEqualTo(UpdateKind.DESTROY);
                    assertThat(destroyResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);

                    var outputsAfterDestroy = stack.getOutputs();
                    assertThat(outputsAfterDestroy).isEmpty();

                } finally {
                    stack.workspace().removeStack(stackName);
                }
            }
        });
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void testOutputStreamAndDelegateIsWritten(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            Consumer<Context> program = ctx -> {
                ctx.export("test", "test");
            };

            var stackName = randomStackName();
            var projectName = "inline_output";
            try (var stack = LocalWorkspace.createStack(projectName, stackName, program,
                    LocalWorkspaceOptions.builder().environmentVariables(env).build())) {
                try {
                    // pulumi preview
                    var outputCalled = new AtomicBoolean(false);
                    var previewResult = stack.preview(PreviewOptions.builder()
                            .onStandardOutput(s -> outputCalled.set(true))
                            .build());
                    assertThat(previewResult.standardOutput()).isNotEmpty();
                    assertThat(outputCalled.get()).isTrue();

                    // pulumi up
                    outputCalled.set(false);
                    var upResult = stack.up(UpOptions.builder()
                            .onStandardOutput(s -> outputCalled.set(true))
                            .build());
                    assertThat(upResult.standardOutput()).isNotEmpty();
                    assertThat(outputCalled.get()).isTrue();

                    // pulumi refresh
                    outputCalled.set(false);
                    var refreshResult = stack.refresh(RefreshOptions.builder()
                            .onStandardOutput(s -> outputCalled.set(true))
                            .build());
                    assertThat(refreshResult.standardOutput()).isNotEmpty();
                    assertThat(outputCalled.get()).isTrue();

                    // pulumi destroy
                    outputCalled.set(false);
                    var destroyResult = stack.destroy(DestroyOptions.builder()
                            .onStandardOutput(s -> outputCalled.set(true))
                            .build());
                    assertThat(destroyResult.standardOutput()).isNotEmpty();
                    assertThat(outputCalled.get()).isTrue();

                } finally {
                    stack.workspace().removeStack(stackName);
                }
            }
        });
    }

    @Test
    void testPulumiVersion() throws Exception {
        try (var workspace = LocalWorkspace.create()) {
            assertThat(workspace.pulumiVersion()).matches("(\\d+\\.)(\\d+\\.)(\\d+)(-.*)?");
        }
    }

    @Test
    void testDetectsProjectSettingsConflict(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        Consumer<Context> program = ctx -> {
        };

        var stackName = randomStackName();
        var projectName = "project_was_overwritten";

        Path workDir = Paths.get(getClass().getResource("/correct_project").toURI());

        var projectSettings = ProjectSettings.createDefault(projectName).toBuilder()
                .description("non-standard description")
                .build();

        assertThrows(ProjectSettingsConflictException.class, () -> {
            LocalWorkspace.createStack(projectName, stackName, program, LocalWorkspaceOptions.builder()
                    .workDir(workDir)
                    .environmentVariables(env)
                    .projectSettings(projectSettings)
                    .build());
        });
    }

    @Test
    void testStateDelete(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        final var type = "test:res";
        Consumer<Context> program = ctx -> {
            new ComponentResource(type, "a");
        };

        var stackName = randomStackName();
        var projectName = "test_state_delete";

        try (var stack = LocalWorkspace.createStack(projectName, stackName, program, LocalWorkspaceOptions.builder()
                .environmentVariables(env)
                .build())) {

            try {
                // pulumi up
                var upResult = stack.up();
                assertThat(upResult.summary().kind()).isEqualTo(UpdateKind.UPDATE);
                assertThat(upResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                assertThat(upResult.summary().resourceChanges().get(OperationType.CREATE)).isEqualTo(2);

                // export state
                var exportResult = stack.exportStack();
                assertThat(exportResult.deployment()).hasEntrySatisfying("resources", value -> {
                    assertThat(value).isInstanceOf(List.class);
                    var resourcesList = (List<?>) value;
                    assertThat(resourcesList).hasSize(2);
                });
                var resources = (List<?>) exportResult.deployment().get("resources");
                var urn = resources.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<?, ?>) item)
                    .filter(itemMap -> itemMap.containsKey("urn") &&
                                    itemMap.get("urn") instanceof String &&
                                    ((String) itemMap.get("urn")).contains(type))
                    .map(itemMap -> (String) itemMap.get("urn"))
                    .findFirst()
                    .orElseThrow();

                // pulumi state delete
                stack.state().delete(urn);

                // test
                exportResult = stack.exportStack();
                assertThat(exportResult.deployment()).hasEntrySatisfying("resources", value -> {
                    assertThat(value).isInstanceOf(List.class);
                    var resourcesList = (List<?>) value;
                    assertThat(resourcesList).hasSize(1);
                });
            }
            finally {
                var destroyResult = stack.destroy();
                assertThat(destroyResult.summary().kind()).isEqualTo(UpdateKind.DESTROY);
                assertThat(destroyResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                stack.workspace().removeStack(stackName);
            }
        }
    }

    @Test
    void testStateDeleteForce(@EnvVars Map<String, String> envVars) throws Exception {
        var env = new HashMap<String, String>(envVars);
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");

        final var type = "test:res";
        Consumer<Context> program = ctx -> {
            new ComponentResource(type, "a", ComponentResourceOptions.builder()
                    .protect(true)
                    .build());
        };

        var stackName = randomStackName();
        var projectName = "test_state_delete_force";

        try (var stack = LocalWorkspace.createStack(projectName, stackName, program, LocalWorkspaceOptions.builder()
                .environmentVariables(env)
                .build())) {

            try {
                // pulumi up
                var upResult = stack.up();
                assertThat(upResult.summary().kind()).isEqualTo(UpdateKind.UPDATE);
                assertThat(upResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                assertThat(upResult.summary().resourceChanges().get(OperationType.CREATE)).isEqualTo(2);

                // export state
                var exportResult = stack.exportStack();
                assertThat(exportResult.deployment()).hasEntrySatisfying("resources", value -> {
                    assertThat(value).isInstanceOf(List.class);
                    var resourcesList = (List<?>) value;
                    assertThat(resourcesList).hasSize(2);
                });
                var resources = (List<?>) exportResult.deployment().get("resources");
                var urn = resources.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<?, ?>) item)
                    .filter(itemMap -> itemMap.containsKey("urn") &&
                                    itemMap.get("urn") instanceof String &&
                                    ((String) itemMap.get("urn")).contains(type))
                    .map(itemMap -> (String) itemMap.get("urn"))
                    .findFirst()
                    .orElseThrow();

                // pulumi state delete
                assertThrows(AutomationException.class, () -> {
                    stack.state().delete(urn);
                });

                // pulumi state delete force
                stack.state().delete(urn, true);

                // test
                exportResult = stack.exportStack();
                assertThat(exportResult.deployment()).hasEntrySatisfying("resources", value -> {
                    assertThat(value).isInstanceOf(List.class);
                    var resourcesList = (List<?>) value;
                    assertThat(resourcesList).hasSize(1);
                });
            }
            finally {
                var destroyResult = stack.destroy();
                assertThat(destroyResult.summary().kind()).isEqualTo(UpdateKind.DESTROY);
                assertThat(destroyResult.summary().result()).isEqualTo(UpdateState.SUCCEEDED);
                stack.workspace().removeStack(stackName);
            }
        }
    }
}
