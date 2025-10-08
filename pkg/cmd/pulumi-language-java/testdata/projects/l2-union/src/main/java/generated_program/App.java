package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.union.Example;
import com.pulumi.union.ExampleArgs;
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
        var stringOrIntegerExample1 = new Example("stringOrIntegerExample1", ExampleArgs.builder()
            .stringOrIntegerProperty(42)
            .build());

        var stringOrIntegerExample2 = new Example("stringOrIntegerExample2", ExampleArgs.builder()
            .stringOrIntegerProperty("forty two")
            .build());

        var mapMapUnionExample = new Example("mapMapUnionExample", ExampleArgs.builder()
            .mapMapUnionProperty(Map.of("key1", Map.of("key1a", "value1a")))
            .build());

        ctx.export("mapMapUnionOutput", mapMapUnionExample.mapMapUnionProperty());
    }
}
