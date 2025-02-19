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
        var protected_ = new Resource("protected", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .protect(true)
                .build());

        var unprotected = new Resource("unprotected", ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .protect(false)
                .build());

        var defaulted = new Resource("defaulted", ResourceArgs.builder()
            .value(true)
            .build());

    }
}
