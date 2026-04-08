package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.CustomTimeouts;
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
        var noTimeouts = new Resource("noTimeouts", ResourceArgs.builder()
            .value(true)
            .build());

        var createOnly = new Resource("createOnly", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .customTimeouts(CustomTimeouts.builder()
                    .create(CustomTimeouts.parseTimeoutString("5m"))
                .build())
                .build());

        var updateOnly = new Resource("updateOnly", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .customTimeouts(CustomTimeouts.builder()
                    .update(CustomTimeouts.parseTimeoutString("10m"))
                .build())
                .build());

        var deleteOnly = new Resource("deleteOnly", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .customTimeouts(CustomTimeouts.builder()
                    .delete(CustomTimeouts.parseTimeoutString("3m"))
                .build())
                .build());

        var allTimeouts = new Resource("allTimeouts", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .customTimeouts(CustomTimeouts.builder()
                    .create(CustomTimeouts.parseTimeoutString("2m"))
                    .update(CustomTimeouts.parseTimeoutString("4m"))
                    .delete(CustomTimeouts.parseTimeoutString("1m"))
                .build())
                .build());

    }
}
