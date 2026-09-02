package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.output.Resource;
import com.pulumi.output.ResourceArgs;
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
        var r = new Resource("r", ResourceArgs.builder()
            .value(1.0)
            .build());

        ctx.export("wrapped", r.output().asSecret());
    }
}
