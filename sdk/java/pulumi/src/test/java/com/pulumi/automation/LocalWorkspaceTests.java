package com.pulumi.automation;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pulumi.Context;
import com.pulumi.core.Output;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import static com.pulumi.automation.ValueOrSecret.secret;
import static com.pulumi.automation.ValueOrSecret.value;

public class LocalWorkspaceTests {

    // local environment, to run locally offline, make sure you set:
    // export PULUMI_BACKEND_URL=file://~
    // export PULUMI_API=file://~
    // pulumi login --local

    @Test
    public void testStackLifecycleInlineProgram(TestInfo testInfo, @TempDir Path tempDir) throws IOException {
        var projectName = safe(testInfo.getDisplayName()) + Tests.randomSuffix();
        var stackName = Tests.randomStackName();

        var projectConfig = ImmutableMap.of(
                "name", projectName,
                "runtime", "java",
                "description", "test",
                "backend", ImmutableMap.of(
                        "url", "file://~"
                )
        );
        var projectFile = Path.of(tempDir.toString(), "Pulumi.json").toFile();
        projectFile.deleteOnExit();
        try (Writer writer = new FileWriter(projectFile)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(projectConfig, writer);
        }
        var stackConfig = ImmutableMap.of(
                "config", ImmutableMap.of(
                        projectName + ":bar", value("abc"),
                        projectName + ":buzz", secret("secret")
                )
        );
        var stackFile = Path.of(tempDir.toString(), String.format("Pulumi.%s.json", stackName)).toFile();
        stackFile.deleteOnExit();
        try (Writer writer = new FileWriter(stackFile)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(stackConfig, writer);
        }

        Consumer<Context> program = ctx -> {
            ctx.export("exp-static", Output.of("foo"));
            ctx.export("exp-cfg", Output.of(ctx.config().require("bar")));
            ctx.export("exp-secret", Output.of(ctx.config().requireSecret("buzz")));
        };

        var workspace = PulumiAuto
                .withProjectSettings(ProjectSettings.builder() // FIXME
                        .name(projectName)
                        .backend("file://~")
                        .build()
                )
                .withEnvironmentVariables(Map.of(
                        "PULUMI_CONFIG_PASSPHRASE", "test"
                ))
                .localWorkspace(LocalWorkspaceOptions.builder()
                        .workDir(tempDir)
                        .program(program)
                        .build()
                );

        var stack = workspace.upsertStack(StackSettings.builder()
                .name(stackName)
                .config(ImmutableMap.of( // FIXME
                        "bar", value("abc"),
                        "buzz", secret("secret")
                ))
                .build()
        );
//        var result = stack.previewAsync();
        var result = stack.upAsync(UpOptions.builder().build());

        result.join();
    }

    private String safe(String displayName) {
        return displayName
                .replace(" ", "")
                .replace(",", "-")
                .replace("(", "-")
                .replace(")", "-");
    }
}
