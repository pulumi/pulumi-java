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

// Scenario 1: adopt a resource in to a component.
class Component1 extends ComponentResource {
    public Component1(String name, ComponentResourceOptions options) {
        super("my:module:Component", name, options);
    }
}


// Scenario 2: adopt this resource into a new parent.
class Component2 extends ComponentResource {
    public Component2(String name, ComponentResourceOptions options) {
        super("my:module:Component2", name, options);
    }
}

// Scenario 3: Make a child resource that is parented by opts instead of 'this'. Fix
// in the next step to be parented by 'this'.
class Component3 extends ComponentResource {
    public Component3(String name, ComponentResourceOptions options) {
        super("my:module:Component3", name, options);
        var component2 = new Component2(name + "-child", options);
    }
}

// Scenario 4: Allow multiple aliases to the same resource.
class Component4 extends ComponentResource {
    public Component4(String name, ComponentResourceOptions options) {
        super("my:module:Component4", name, ComponentResourceOptions.Empty);

    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step0");

            var emptyOptions = ComponentResourceOptions.Empty;

            new Resource("res", emptyOptions);

            var component = new Component1("comp", emptyOptions);

            new Component2("unparented", emptyOptions);

            new Component3("parented-by-stack", emptyOptions);
            new Component3("parented-by-component", ComponentResourceOptions.builder().parent(component).build());

            new Component4("duplicated-alias", ComponentResourceOptions.builder().parent(component).build());
        });
    }
}
