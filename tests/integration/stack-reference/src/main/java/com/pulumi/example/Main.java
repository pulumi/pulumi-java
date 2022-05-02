package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step0");
            var isLocal = ctx.config().getBoolean("local").orElse(false);
            final String slug;
            if (isLocal) {
                slug = ctx.stackName();
            } else {
                var org = ctx.config().require("org");
                slug = String.format(
                        "%s/%s/%s", org, ctx.projectName(), ctx.stackName()
                );
            }
            new StackReference(slug);

            ctx.export("val", Output.of(List.of("a", "b")));
        });
    }
}
