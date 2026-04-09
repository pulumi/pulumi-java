package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
import com.pulumi.resources.CustomResourceOptions;
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
        var withV2 = new Resource("withV2", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .version("2.0.0")
                .build());

        var withV26 = new Resource("withV26", ResourceArgs.builder()
            .value(false)
            .build(), CustomResourceOptions.builder()
                .version("26.0.0")
                .build());

        var withDefault = new Resource("withDefault", ResourceArgs.builder()
            .value(true)
            .build());

    }
}
