package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Provider;
import com.pulumi.resources.CustomResourceOptions;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var provider = new Provider("provider");

        var parent1 = new com.pulumi.simple.Resource("parent1", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .provider(provider)
                .build());

        // This should inherit the explicit provider from parent1
        var child1 = new com.pulumi.simple.Resource("child1", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent1)
                .build());

        var parent2 = new com.pulumi.primitive.Resource("parent2", com.pulumi.primitive.ResourceArgs.builder()
            .boolean_(false)
            .float_(0.0)
            .integer(0)
            .string("")
            .numberArray()
            .booleanMap(Map.ofEntries(
            ))
            .build());

        // This _should not_ inherit the provider from parent2 as it is a default provider.
        var child2 = new com.pulumi.simple.Resource("child2", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent2)
                .build());

        // This _should not_ inherit the provider from parent1 as its from the wrong package.
        var child3 = new com.pulumi.primitive.Resource("child3", com.pulumi.primitive.ResourceArgs.builder()
            .boolean_(false)
            .float_(0.0)
            .integer(0)
            .string("")
            .numberArray()
            .booleanMap(Map.ofEntries(
            ))
            .build(), CustomResourceOptions.builder()
                .parent(parent1)
                .build());

    }
}
