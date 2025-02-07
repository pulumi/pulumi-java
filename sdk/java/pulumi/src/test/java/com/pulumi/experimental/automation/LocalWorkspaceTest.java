// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import com.pulumi.Context;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(LocalBackendExtension.class)
public class LocalWorkspaceTest {
    private static String randomStackName() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void testStackLifecycleInlineProgram(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            var testPassed = false;

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
                    // assertThat(upResult.getSummary().getKind()).isEqualTo(UpdateKind.UPDATE);
                    // assertThat(upResult.getSummary().getResult()).isEqualTo(UpdateState.SUCCEEDED);
                    // assertThat(upResult.getOutputs().size()).isEqualTo(3);

                    // // exp_static
                    // var expStaticValue = upResult.getOutputs().get("exp_static");
                    // assertThat(expStaticValue.getValue()).isEqualTo("foo");
                    // assertThat(expStaticValue.isSecret()).isFalse();

                    // // exp_cfg
                    // var expConfigValue = upResult.getOutputs().get("exp_cfg");
                    // assertThat(expConfigValue.getValue()).isEqualTo("abc");
                    // assertThat(expConfigValue.isSecret()).isFalse();

                    // // exp_secret
                    // var expSecretValue = upResult.getOutputs().get("exp_secret");
                    // assertThat(expSecretValue.getValue()).isEqualTo("secret");
                    // assertThat(expSecretValue.isSecret()).isTrue();

                    // pulumi preview
                    // var previewResult = stack.preview();
                    // var sameCount = previewResult.getChangeSummary().get(OperationType.SAME);
                    // assertThat(sameCount).isEqualTo(1);

                    // // pulumi refresh
                    // var refreshResult = stack.refresh();
                    // assertThat(refreshResult.getSummary().getKind()).isEqualTo(UpdateKind.REFRESH);
                    // assertThat(refreshResult.getSummary().getResult()).isEqualTo(UpdateState.SUCCEEDED);

                    // // pulumi destroy
                    // var destroyResult = stack.destroy();
                    // assertThat(destroyResult.getSummary().getKind()).isEqualTo(UpdateKind.DESTROY);
                    // assertThat(destroyResult.getSummary().getResult()).isEqualTo(UpdateState.SUCCEEDED);
                } finally {
                    if (testPassed) {
                        stack.getWorkspace().removeStack(stackName);
                    }
                }
            }
        });
    }

    @Disabled
    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testStackLifecycleLocalProgram(@EnvVars Map<String, String> envVars) {
        assertDoesNotThrow(() -> {
            var env = new HashMap<String, String>(envVars);
            env.put("PULUMI_CONFIG_PASSPHRASE", "test");

            var testPassed = false;

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
                    assertThat(upResult.getSummary().getKind()).isEqualTo(UpdateKind.UPDATE);
                    assertThat(upResult.getSummary().getResult()).isEqualTo(UpdateState.SUCCEEDED);
                    assertThat(upResult.getOutputs().size()).isEqualTo(3);

                    // exp_static
                    var expStaticValue = upResult.getOutputs().get("exp_static");
                    assertThat(expStaticValue.getValue()).isEqualTo("foo");
                    assertThat(expStaticValue.isSecret()).isFalse();

                    // exp_cfg
                    var expConfigValue = upResult.getOutputs().get("exp_cfg");
                    assertThat(expConfigValue.getValue()).isEqualTo("abc");
                    assertThat(expConfigValue.isSecret()).isFalse();

                    // exp_secret
                    var expSecretValue = upResult.getOutputs().get("exp_secret");
                    assertThat(expSecretValue.getValue()).isEqualTo("secret");
                    assertThat(expSecretValue.isSecret()).isTrue();

                    // pulumi preview
                    var previewResult = stack.preview();
                    var sameCount = previewResult.getChangeSummary().get(OperationType.SAME);
                    assertThat(sameCount).isEqualTo(1);

                    // pulumi refresh
                    var refreshResult = stack.refresh();
                    assertThat(refreshResult.getSummary().getKind()).isEqualTo(UpdateKind.REFRESH);
                    assertThat(refreshResult.getSummary().getResult()).isEqualTo(UpdateState.SUCCEEDED);

                    // pulumi destroy
                    var destroyResult = stack.destroy();
                    assertThat(destroyResult.getSummary().getKind()).isEqualTo(UpdateKind.DESTROY);
                    assertThat(destroyResult.getSummary().getResult()).isEqualTo(UpdateState.SUCCEEDED);


                } finally {
                    if (testPassed) {
                        stack.getWorkspace().removeStack(stackName);
                    }
                }
            }
        });
    }
}
