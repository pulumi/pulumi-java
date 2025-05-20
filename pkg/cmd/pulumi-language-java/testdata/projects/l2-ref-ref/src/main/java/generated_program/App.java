package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.refref.Resource;
import com.pulumi.refref.ResourceArgs;
import com.pulumi.refref.inputs.DataArgs;
import com.pulumi.refref.inputs.InnerDataArgs;
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
            .data(DataArgs.builder()
                .innerData(InnerDataArgs.builder()
                    .boolean_(false)
                    .float_(2.17)
                    .integer(-12)
                    .string("Goodbye")
                    .boolArray(                    
                        false,
                        true)
                    .stringMap(Map.ofEntries(
                        Map.entry("two", "turtle doves"),
                        Map.entry("three", "french hens")
                    ))
                    .build())
                .boolean_(true)
                .float_(4.5)
                .integer(1024)
                .string("Hello")
                .boolArray()
                .stringMap(Map.ofEntries(
                    Map.entry("x", "100"),
                    Map.entry("y", "200")
                ))
                .build())
            .build());

    }
}
