package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
        var failing = new com.pulumi.fail_on_create.Resource("failing", com.pulumi.fail_on_create.ResourceArgs.builder()
            .value(false)
            .build());

        var dependent = new com.pulumi.simple.Resource("dependent", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .dependsOn(failing)
                .build());

        var dependent_on_output = new com.pulumi.simple.Resource("dependent_on_output", com.pulumi.simple.ResourceArgs.builder()
            .value(failing.value())
            .build());

        var independent = new com.pulumi.simple.Resource("independent", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build());

        var double_dependency = new com.pulumi.simple.Resource("double_dependency", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .dependsOn(                
                    independent,
                    dependent_on_output)
                .build());

    }
}
