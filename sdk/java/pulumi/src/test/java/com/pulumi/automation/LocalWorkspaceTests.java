package com.pulumi.automation;

import com.pulumi.Context;
import com.pulumi.core.Output;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static com.pulumi.automation.ValueOrSecret.secret;
import static com.pulumi.automation.ValueOrSecret.value;


public class LocalWorkspaceTests {

    @Test
    public void testStackLifecycleInlineProgram(TestInfo testInfo, @TempDir Path tempDir) {
        Consumer<Context> program = ctx -> {
            ctx.export("exp-static", Output.of("foo"));
            ctx.export("exp-cfg", Output.of(ctx.config().require("bar")));
            ctx.export("exp-secret", Output.of(ctx.config().requireSecret("buzzz")));
        };

        var workspace = PulumiAuto
                .withProjectSettings(ProjectSettings.builder()
                        .name(testInfo.getDisplayName() + Tests.randomSuffix())
                        .build()
                )
                .withEnvironmentVariables(Map.of(
                        "PULUMI_CONFIG_PASSPHRASE", "test"
                ))
                .withInlineProgram(program)
                .localWorkspace(LocalWorkspaceOptions.builder()
                        .workDir(tempDir)
                        .build()
                );

        var stack = workspace.upsertStack(StackSettings.builder()
                .name(Tests.randomStackName())
                .config(Map.of(
                        "bar", value("abc"),
                        "buzz", secret("secret")
                ))
                .build()
        );
        var result = stack.up(UpOptions.builder().build());
    }
}
