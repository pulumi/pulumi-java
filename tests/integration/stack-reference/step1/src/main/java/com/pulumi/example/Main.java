package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;

import java.util.Objects;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step1");
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
            var a = new StackReference(slug);

            var oldVal = (List<String>) a.getValueAsync(Output.of("val")).join();
            if (oldVal.size() != 2
                    || !Objects.equals(oldVal.get(0), "a")
                    || !Objects.equals(oldVal.get(1), "b")
            ) {
                throw new RuntimeException(String.format("Invalid result: '%s'", oldVal));
            }

            ctx.export("val2", Output.ofSecret(List.of("a", "b")));
            return ctx.exports();
        });
    }
}