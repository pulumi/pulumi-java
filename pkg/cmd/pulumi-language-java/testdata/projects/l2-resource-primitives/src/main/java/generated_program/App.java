package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.primitive.Resource;
import com.pulumi.primitive.ResourceArgs;
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
        var res = new Resource("res", ResourceArgs.builder()
            .boolean_(true)
            .float_(3.14)
            .integer(42)
            .string("hello")
            .numberArray(            
                -1.0,
                0.0,
                1.0)
            .booleanMap(Map.ofEntries(
                Map.entry("t", true),
                Map.entry("f", false)
            ))
            .build());

    }
}
