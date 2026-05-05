package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
        final var config = ctx.config();
        final var aSecret = config.requireSecret("aSecret");
        final var notSecret = config.require("notSecret");
        ctx.export("roundtripSecret", aSecret);
        ctx.export("roundtripNotSecret", notSecret);
        ctx.export("double", aSecret.asSecret());
        ctx.export("open", aSecret.asPlaintext());
        ctx.export("close", Output.ofSecret(notSecret));
    }
}
