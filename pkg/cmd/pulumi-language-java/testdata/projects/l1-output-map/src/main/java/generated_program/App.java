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
        ctx.export("empty", Map.ofEntries(
        ));
        ctx.export("strings", Map.ofEntries(
            Map.entry("greeting", "Hello, world!"),
            Map.entry("farewell", "Goodbye, world!")
        ));
        ctx.export("numbers", Map.ofEntries(
            Map.entry("1", 1),
            Map.entry("2", 2)
        ));
        ctx.export("keys", Map.ofEntries(
            Map.entry("my.key", 1),
            Map.entry("my-key", 2),
            Map.entry("my_key", 3),
            Map.entry("MY_KEY", 4),
            Map.entry("mykey", 5),
            Map.entry("MYKEY", 6)
        ));
    }
}
