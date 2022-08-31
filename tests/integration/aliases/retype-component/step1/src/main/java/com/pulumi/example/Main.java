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


// Scenario: change the type of a component.
class Component1 extends ComponentResource {
    public Component1(String name, ComponentResourceOptions options) {
        super("my:module:ComponentFourWithDifferentTypeName", name,

                // Add an alias that references the old type of this resource
                // and then make the super() call with the new type of this resource and the added alias.
                ComponentResourceOptions.merge(
                        options,
                        ComponentResourceOptions.builder()
                                .aliases(Alias.builder().type("my:module:ComponentFour").build())
                                .build()
                )
        );

        // The child resource will also pick up an implicit alias due to the new type of the component it is parented to.
        var resource = new Resource("otherchild", ComponentResourceOptions.builder()
                .parent(this)
                .build()
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step1");
            
            new Component1("component1", ComponentResourceOptions.Empty);
        });
    }
}
