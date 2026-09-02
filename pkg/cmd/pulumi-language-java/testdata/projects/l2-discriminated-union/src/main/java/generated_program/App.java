package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.discriminatedunion.Example;
import com.pulumi.discriminatedunion.ExampleArgs;
import com.pulumi.discriminatedunion.inputs.VariantOneArgs;
import com.pulumi.discriminatedunion.inputs.VariantTwoArgs;
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
        var example1 = new Example("example1", ExampleArgs.builder()
            .unionOf(VariantOneArgs.builder()
                .field1("v1 union")
                .build())
            .arrayOfUnionOf(VariantOneArgs.builder()
                .field1("v1 array(union)")
                .build())
            .build());

        var example2 = new Example("example2", ExampleArgs.builder()
            .unionOf(VariantTwoArgs.builder()
                .field2("v2 union")
                .build())
            .arrayOfUnionOf(            
                VariantTwoArgs.builder()
                    .field2("v2 array(union)")
                    .build(),
                VariantOneArgs.builder()
                    .field1("v1 array(union)")
                    .build())
            .build());

    }
}
