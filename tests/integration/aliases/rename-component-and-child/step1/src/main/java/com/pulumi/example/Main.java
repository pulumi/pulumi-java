package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.core.Alias;


import java.util.Objects;
import java.util.List;

class ChildResource extends ComponentResource {
    public ChildResource(String name, ComponentResourceOptions options) {
        super("my:module:ChildResource", name, options);
    }
}

// Scenario: rename parent and child at the same time.
class ParentResource extends ComponentResource {
    public ParentResource(String name, ComponentResourceOptions options) {
        super("my:module:ParentResource", name, options);

        var otherChild = new ChildResource("other-child-renamed",
                ComponentResourceOptions.builder()
                        .parent(this)
                        .aliases(Alias.builder().name("other-child").build())
                        .build()
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step1");
            var res1 = new ParentResource("new-resource-name",
                    ComponentResourceOptions.builder()
                            .aliases(Alias.builder().name("resource-name").build())
                            .build()
            );
        });
    }
}
