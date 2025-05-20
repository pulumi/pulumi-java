package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;
import com.pulumi.resources.StackReferenceArgs;
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
        var ref = new StackReference("ref", StackReferenceArgs.builder()
            .name("organization/other/dev")
            .build());

        ctx.export("plain", ref.output("plain"));
        ctx.export("secret", ref.output("secret"));
    }
}
