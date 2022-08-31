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

// Scenario: nested parents changing types.
class Component1 extends ComponentResource {
    public Component1(String name, ComponentResourceOptions options) {
        super("my:module:Component1-v0", name, options);

        var resource = new Resource("otherchild", ComponentResourceOptions.builder()
                .parent(this)
                .build()
        );
    }
}

class Component1Parent extends ComponentResource {

    public Component1Parent(String name, ComponentResourceOptions options) {
        super("my:module:Component1Parent-v0", name, options);

        var child = new Component1("child", ComponentResourceOptions.builder()
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
