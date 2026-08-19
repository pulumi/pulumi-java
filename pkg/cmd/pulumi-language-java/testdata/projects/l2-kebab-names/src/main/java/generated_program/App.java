package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kebabnames.kebabmodule.SomeResource;
import com.pulumi.kebabnames.kebabmodule.SomeResourceArgs;
import com.pulumi.kebabnames.kebabmodule.inputs.NestedInputArgs;
import com.pulumi.kebabnames.kebabmodule.AnotherResource;
import com.pulumi.kebabnames.kebabmodule.AnotherResourceArgs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // The package name and module name are kebab-case. Resource and object type names cannot be
        // kebab-case yet (the metaschema forbids hyphens in the member segment of a token), and kebab-case
        // property names are not yet handled by all code generators.
        var first = new SomeResource("first", SomeResourceArgs.builder()
            .theInput(true)
            .nested(NestedInputArgs.builder()
                .nestedValue("nested")
                .build())
            .build());

        var second = new AnotherResource("second", AnotherResourceArgs.builder()
            .theInput(first.theOutput().applyValue(_theOutput -> _theOutput.nestedOutput()))
            .build());

    }
}
