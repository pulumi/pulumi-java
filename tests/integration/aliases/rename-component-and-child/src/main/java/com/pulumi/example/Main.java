package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;


class ChildResource extends ComponentResource {
    public ChildResource(String name, ComponentResourceOptions options) {
        super("my:module:ChildResource", name, options);
    }
}

// Scenario: rename parent and child at the same time.
class ParentResource extends ComponentResource {
    public ParentResource(String name, ComponentResourceOptions options) {
        super("my:module:ParentResource", name, options);

        var otherChild = new ChildResource("other-child",
                ComponentResourceOptions.builder()
                        .parent(this)
                        .build()
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step0");
            var res1 = new ParentResource("resource-name", ComponentResourceOptions.Empty);
        });
    }
}
