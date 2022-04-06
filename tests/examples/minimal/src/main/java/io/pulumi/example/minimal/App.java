package io.pulumi.example.minimal;

import io.pulumi.Pulumi;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(ctx -> {
            var config = ctx.config();
            var name = config.require("name");
            var secret = config.require("secret");
            ctx.log().info("Hello, %s!%n", name);
            ctx.log().info("Psst, %s%n", secret);
            return ctx.exports();
        });
        System.exit(exitCode);
    }
}
