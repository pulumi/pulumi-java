package myproject;

import io.pulumi.Config;
import io.pulumi.Pulumi;
import io.pulumi.core.Output;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(() -> {
            return Map.of("output1", Output.of("example"));
        });
        System.exit(exitCode);
    }
}
