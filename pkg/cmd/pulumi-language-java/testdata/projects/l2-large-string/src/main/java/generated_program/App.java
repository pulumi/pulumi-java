package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.large.String;
import com.pulumi.large.StringArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(java.lang.String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var res = new String("res", StringArgs.builder()
            .value("hello world")
            .build());

        ctx.export("output", res.value());
    }
}
