package com.pulumi.example.provider;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;

public class App {
    public static void main(String[] args) {
        System.out.println("Hello, Provider!");
        // Pulumi.run(ctx -> {
        //     ctx.export("exampleOutput", Output.of("example"));
        // });
    }
}
