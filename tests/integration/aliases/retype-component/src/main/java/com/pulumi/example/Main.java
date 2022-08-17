package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

import java.util.List;

class Resource extends ComponentResource {
    public Resource(String name, ComponentResourceOptions options) {
        super("my:module:Resource", name, options);
    }
}

// Scenario: change the type of a component.
class Component1 extends ComponentResource {
    public Component1(String name, ComponentResourceOptions options) {
        super("my:module:ComponentFour", name, options);

        var resource = new Resource("otherchild", ComponentResourceOptions.builder()
                .parent(this)
                .build()
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step0");

            new Component1("component1", ComponentResourceOptions.Empty);
        });
    }
}
