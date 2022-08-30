package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.core.Alias;


import java.util.Objects;
import java.util.List;

class Resource extends ComponentResource {
    public Resource(String name, ComponentResourceOptions options) {
        super("my:module:Resource", name, options);
    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step1");

            // Scenario: rename a resource.
            // This resource was previously named `res1`, we'll alias to the old name.
            var res1 = new Resource("newres1",
                    ComponentResourceOptions.builder()
                            .aliases(Alias.builder().name("res1").build())
                            .build()
            );
        });
    }
}
