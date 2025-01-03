package com.pulumi.example;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Pulumi.run(ctx -> {
            ctx.log().info("step2");
            var slug = String.format(
                    "%s/%s/%s", ctx.organizationName(), ctx.projectName(), ctx.stackName()
            );
            var a = new StackReference(slug);

            var gotExpectedError = false;
            try {
                 a.getValueAsync(Output.of("val2")).join();
            } catch (Exception e) {
                gotExpectedError = true;
            }

            if (!gotExpectedError) {
                throw new RuntimeException("Expected to get error trying to read secret from stack reference.");
            }
            ctx.export("gotExpectedError", Output.of(gotExpectedError));
        });
    }
}
