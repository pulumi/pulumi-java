package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
        // Keywords in various languages should be renamed and work.
        final var class = "class_output_string";

        final var export = "export_output_string";

        final var import = "import_output_string";

        final var mod = "mod_output_string";

        final var object = Map.of("object", "object_output_string");

        final var self = "self_output_string";

        final var this = "this_output_string";

        final var if = "if_output_string";

        ctx.export("class", class_);
        ctx.export("export", export);
        ctx.export("import", import_);
        ctx.export("mod", mod);
        ctx.export("object", object);
        ctx.export("self", self);
        ctx.export("this", this_);
        ctx.export("if", if_);
    }
}
