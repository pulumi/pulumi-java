package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.core.Alias;
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
        var parent = new Resource("parent", ResourceArgs.builder()
            .value(true)
            .build());

        var aliasURN = new Resource("aliasURN", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent)
                .aliases(Alias.withUrn("urn:pulumi:test::l2-resource-option-alias::simple:index:Resource::aliasURN"))
                .build());

        var aliasNewName = new Resource("aliasNewName", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .aliases(Alias.builder()
                    .name("aliasName").build())
                .build());

        var aliasNoParent = new Resource("aliasNoParent", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent)
                .aliases(Alias.builder()
                    .noParent().build())
                .build());

        var aliasParent = new Resource("aliasParent", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .parent(parent)
                .aliases(Alias.builder()
                    .parent(aliasURN).build())
                .build());

    }
}
