package io.pulumi.example.minimal;

import io.pulumi.Config;
import io.pulumi.Pulumi;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(() -> {
            var config = Config.of();
            var name = config.require("name");
            var secret = config.require("secret");
            System.out.printf("Hello, %s!%n", name);
            System.out.printf("Psst, %s%n", secret);

            return Map.of();
        });
        System.exit(exitCode);
    }
}
