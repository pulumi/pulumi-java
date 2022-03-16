package io.pulumi.example.minimal;

import io.pulumi.Config;
import io.pulumi.Stack;

public final class MyStack extends Stack {
    public MyStack() {
        var config = Config.of();
        var name = config.require("name");
        var secret = config.require("secret");
        System.out.printf("Hello, %s!%n", name);
        System.out.printf("Psst, %s%n", secret);
    }
}
