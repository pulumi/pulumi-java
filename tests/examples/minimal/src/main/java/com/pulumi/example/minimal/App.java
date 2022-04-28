package com.pulumi.example.minimal;

import com.pulumi.Pulumi;

public class App {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            var log = ctx.log();
            var config = ctx.config();
            var name = config.require("name");
            var secret = config.require("secret");
            log.info("Hello, %s!%n", name);
            log.info("Psst, %s%n", secret);
            return ctx.exports();
        });
    }
}
