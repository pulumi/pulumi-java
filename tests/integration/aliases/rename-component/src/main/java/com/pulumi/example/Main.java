package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

import java.util.List;

class ChildResource extends ComponentResource {
    public ChildResource(String name, ComponentResourceOptions options) {
        super("my:module:ChildResource", name, options);
    }
}

// Scenario: rename a component.
class ParentResource extends ComponentResource {
    public ParentResource(String name, ComponentResourceOptions options) {
        super("my:module:ParentResource", name, options);

        // Note that both un-prefixed and parent-name-prefixed child names are supported. For the later, the implicit
        // alias inherited from the parent alias will be included replacing the name prefix to match the parent alias name.
        var res1 = new ChildResource(name + "-child1",
                ComponentResourceOptions.builder()
                        .parent(this)
                        .build()
        );

        var res2 = new ChildResource("other-child",
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
            var res1 = new ParentResource("resource-name", ComponentResourceOptions.builder().build());
        });
    }
}