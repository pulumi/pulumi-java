package myproject;

import com.pulumi.Context;
import com.pulumi.automation.LocalWorkspace;
import com.pulumi.automation.LocalWorkspaceOptions;
import com.pulumi.random.Password;
import com.pulumi.random.PasswordArgs;
import com.pulumi.random.Uuid;
import com.pulumi.random.UuidArgs;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Consumer;

public class App {
    public static void main(String[] args) throws Exception {
        var tempDir = Files.createTempDirectory("pulumi-stale-ref-test-");
        var env = new HashMap<String, String>();
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");
        env.put("PULUMI_BACKEND_URL", "file:///" + tempDir.toAbsolutePath());

        var projectName = "stale-ref-project";
        var opts = LocalWorkspaceOptions.builder().environmentVariables(env).build();

        Consumer<Context> program1 = ctx -> {
            new Password("my-password", PasswordArgs.builder().length(16.0).build());
        };

        Consumer<Context> program2 = ctx -> {
            new Uuid("my-uuid", UuidArgs.Empty);
        };

        try (var stack = LocalWorkspace.createOrSelectStack(projectName, "stale-ref-test-1", program1, opts)) {
            try {
                stack.preview();
                System.out.println("First preview succeeded");
            } finally {
                try { stack.workspace().removeStack("stale-ref-test-1"); } catch (Exception ignored) {}
            }
        }

        try (var stack = LocalWorkspace.createOrSelectStack(projectName, "stale-ref-test-2", program2, opts)) {
            try {
                stack.preview();
                System.out.println("Second preview succeeded");
            } finally {
                try { stack.workspace().removeStack("stale-ref-test-2"); } catch (Exception ignored) {}
            }
        }
    }
}
