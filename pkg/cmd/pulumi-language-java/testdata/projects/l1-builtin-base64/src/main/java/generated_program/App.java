package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
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
        final var input = config.require("input");
        final var bytes = new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);

        ctx.export("data", bytes);
        ctx.export("roundtrip", Base64.getEncoder().encodeToString(bytes.getBytes(StandardCharsets.UTF_8)));
    }
}
