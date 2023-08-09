package com.pulumi.automation;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pulumi.automation.internal.LanguageRuntimeContext;
import com.pulumi.automation.internal.LanguageRuntimeServer;
import com.pulumi.automation.internal.LanguageRuntimeService;
import com.pulumi.automation.internal.Shell;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;

import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PulumiAutoTest {

    private static LanguageRuntimeServer server;

    @BeforeAll
    static void init() {
        var logger = logger(Level.FINEST);
        var context = new LanguageRuntimeContext(ctx -> {});
        var service = new LanguageRuntimeService(logger, context);
        server = new LanguageRuntimeServer(logger, service);
        server.start();
    }

    @AfterAll
    static void cleanup() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void test() throws IOException, InterruptedException {
        var project = ImmutableMap.of(
                "name", "test",
                "runtime", "java",
                "description", "test"
        );
        try (Writer writer = new FileWriter("Pulumi.json")) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(project, writer);
        }
        var stackName = "test";
        var stack = ImmutableMap.of();
        try (Writer writer = new FileWriter(String.format("Pulumi.%s.json", stackName))) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(stack, writer);
        }

        var shell = new Shell();
        var stackInitOrSelect = shell.run(
                "pulumi",
                "stack",
                "select",
                stackName
        );
        var exitCode = stackInitOrSelect.join();
        assertEquals(0, exitCode);

        var preview = shell.run(
                "pulumi",
                "preview",
                String.format("--stack=%s", "test"),
                String.format("--client=127.0.0.1:%d", server.port()),
                String.format("--exec-kind=%s", "auto.inline")
        );
        exitCode = preview.join();
        assertEquals(0, exitCode);
    }
}

