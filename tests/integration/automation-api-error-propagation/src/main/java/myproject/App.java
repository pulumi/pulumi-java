package myproject;

import com.pulumi.Context;
import com.pulumi.automation.AutomationException;
import com.pulumi.automation.LocalWorkspace;
import com.pulumi.automation.LocalWorkspaceOptions;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Consumer;

public class App {
    public static void main(String[] args) throws Exception {
        var tempDir = Files.createTempDirectory("pulumi-error-propagation-test-");
        var env = new HashMap<String, String>();
        env.put("PULUMI_CONFIG_PASSPHRASE", "test");
        env.put("PULUMI_BACKEND_URL", "file:///" + tempDir.toAbsolutePath());

        var projectName = "error-propagation-project";
        var opts = LocalWorkspaceOptions.builder().environmentVariables(env).build();

        Consumer<Context> failingProgram = ctx -> {
            throw new RuntimeException("intentional test failure");
        };

        try (var stack = LocalWorkspace.createOrSelectStack(projectName, "error-propagation-test", failingProgram, opts)) {
            try {
                stack.preview();
                System.err.println("FAIL: preview() should have thrown an AutomationException");
                System.exit(1);
            } catch (AutomationException e) {
                System.out.println("Error propagation works");
            } finally {
                try { stack.workspace().removeStack("error-propagation-test"); } catch (Exception ignored) {}
            }
        }
    }
}
