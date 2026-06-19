package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.constant.Resource;
import com.pulumi.constant.ResourceArgs;
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
        var first = new Resource("first", ResourceArgs.builder()
            .kind("Constant")
            .build());

        ctx.export("kind", first.kind());
    }
}
