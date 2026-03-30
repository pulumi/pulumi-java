package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
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
        var ignoreChanges = new Resource("ignoreChanges", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .ignoreChanges("value")
                .build());

        var notIgnoreChanges = new Resource("notIgnoreChanges", ResourceArgs.builder()
            .value(true)
            .build());

    }
}
