package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.keywords.SomeResource;
import com.pulumi.keywords.SomeResourceArgs;
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
        var firstResource = new SomeResource("firstResource", SomeResourceArgs.builder()
            .builtins("builtins")
            .property("property")
            .build());

        var secondResource = new SomeResource("secondResource", SomeResourceArgs.builder()
            .builtins(firstResource.builtins())
            .property(firstResource.property())
            .build());

    }
}
