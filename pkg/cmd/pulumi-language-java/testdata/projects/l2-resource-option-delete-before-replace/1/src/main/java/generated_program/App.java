package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
        // Stage 1: Change properties to trigger replacements
        // Resource with deleteBeforeReplace option - should delete before creating
        var withOption = new Resource("withOption", ResourceArgs.builder()
            .value(false)
            .build(), CustomResourceOptions.builder()
                .deleteBeforeReplace(true)
                .replaceOnChanges("value")
                .build());

        // Resource without deleteBeforeReplace - should create before deleting (default)
        var withoutOption = new Resource("withoutOption", ResourceArgs.builder()
            .value(false)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

    }
}
