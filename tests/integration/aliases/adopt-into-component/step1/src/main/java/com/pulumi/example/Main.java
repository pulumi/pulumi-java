package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.core.Alias;

import java.util.stream.Collectors;
import java.util.Objects;
import java.util.List;

class Resource extends ComponentResource {
    public Resource(String name, ComponentResourceOptions options) {
        super("my:module:Resource", name, options);
    }
}

// Scenario 1: adopt a resource in to a component. The component author is the same as the component user, and changes
// the component to be able to adopt the resource that was previously defined separately.
class Component1 extends ComponentResource {
    public Component1(String name, String stackName, String projectName, ComponentResourceOptions options) {
        super("my:module:Component", name, options);

        var adoptedResourceType = "my:module:Resource";
        var adoptedResourceName = "res";

        // The resource creation was moved from top level to inside the component.
        var adoptedResource = new Resource(
                name + "-child",
                ComponentResourceOptions.builder()
                    // With a new parent
                    .parent(this)
                    // But with an alias provided based on knowing where the resource existing before - in this case at top
                    // level.  We use an absolute URN instead of a relative `Alias` because we are referencing a fixed resource
                    // that was in some arbitrary other location in the hierarchy prior to being adopted into this component.
                    .aliases(Alias.withUrn(String.format("urn:pulumi:%s::%s::%s::%s", stackName, projectName, adoptedResourceType, adoptedResourceName)))
                    .build()
        );
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

        var aliasWithParents = options.getParent()
                .map(parent -> Alias.builder().parent(parent).build())
                .orElse(Alias.noParent());

        var component2 = new com.pulumi.example.Component2(name + "-child", ComponentResourceOptions.builder()
                .aliases(aliasWithParents)
                .parent(this)
                .build()
        );
    }
}

// Scenario 4: Allow multiple aliases to the same resource.
class Component4 extends ComponentResource {
    public Component4(String name, ComponentResourceOptions options) {
        super("my:module:Component4", name,
                ComponentResourceOptions.merge(
                        ComponentResourceOptions.builder()
                                .aliases(Alias.noParent(), Alias.noParent())
                                .build(),
                        options
                )
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step1");

            var emptyOptions = ComponentResourceOptions.Empty;

            var component = new Component1("comp", ctx.stackName(), ctx.projectName(), emptyOptions);

            new Component2("unparented", ComponentResourceOptions.builder()
                    .aliases(Alias.noParent())
                    .parent(component)
                    .build()
            );

            new Component3("parented-by-stack", emptyOptions);
            new Component3("parented-by-component", ComponentResourceOptions.builder().parent(component).build());

            new Component4("duplicated-alias", ComponentResourceOptions.builder().parent(component).build());
        });
    }
}
