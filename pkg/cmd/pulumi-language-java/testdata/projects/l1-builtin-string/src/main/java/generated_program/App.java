package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.codegen.internal.Strings;
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
        final var aString = config.require("aString");
        ctx.export("lengthResult", Strings.length(aString));
        ctx.export("splitResult", Arrays.asList(aString.split("-")));
        ctx.export("joinResult", String.join("|", Arrays.asList(aString.split("-"))));
        ctx.export("interpolateResult", String.format("prefix-%s", aString));
    }
}
