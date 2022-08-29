package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.core.Alias;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Resource extends ComponentResource {
    public Resource(String name, ComponentResourceOptions options) {
        super("my:module:Resource", name, options);
    }
}


// Scenario: nested parents changing types.
class Component1 extends ComponentResource {
    private static List<Output<Alias>> generateAliases() {
        return IntStream
                .range(0, 100)
                .mapToObj(i ->
                        Output.of(
                                Alias.builder()
                                        .type("my:module:Component1-v" + i)
                                        .build()
                        )
                )
                .collect(Collectors.toList());
    }

    public Component1(String name, ComponentResourceOptions options) {
        super(
                "my:module:Component1-v100", name,
                ComponentResourceOptions.merge(
                        options,

                        // Add an alias that references the old type of this resource
                        // and then make the super() call with the new type of this resource and the added alias.
                        ComponentResourceOptions.builder().aliases(generateAliases()).build()
                )
        );

        // The child resource will also pick up an implicit alias due to the new type of the component it is parented to.
        var resource = new Resource("otherchild", ComponentResourceOptions.builder()
                .parent(this)
                .build()
        );
    }
}

class Component1Parent extends ComponentResource {

    private static List<Output<Alias>> generateAliases() {
        return IntStream
                .range(0, 100)
                .mapToObj(i ->
                        Output.of(
                                Alias.builder()
                                        .type("my:module:Component1Parent-v" + i)
                                        .build()
                        )
                )
                .collect(Collectors.toList());
    }

    public Component1Parent(String name, ComponentResourceOptions options) {
        super("my:module:Component1Parent-v100", name, options);

        var child = new Component1("child", ComponentResourceOptions.merge(
                options,

                // Add an alias that references the old type of this resource
                // and then make the super() call with the new type of this resource and the added alias.
                ComponentResourceOptions.builder().aliases(generateAliases()).build()
        ));
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
