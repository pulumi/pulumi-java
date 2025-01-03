package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Provider;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
import com.pulumi.resources.CustomResourceOptions;
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

        var parent1 = new Resource("parent1", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .provider(provider)
                .build());

        var child1 = new Resource("child1", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent1)
                .build());

        var orphan1 = new Resource("orphan1", ResourceArgs.builder()
            .value(true)
            .build());

        var parent2 = new Resource("parent2", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .protect(true)
                .build());

        var child2 = new Resource("child2", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent2)
                .build());

        var orphan2 = new Resource("orphan2", ResourceArgs.builder()
            .value(true)
            .build());

    }
}
