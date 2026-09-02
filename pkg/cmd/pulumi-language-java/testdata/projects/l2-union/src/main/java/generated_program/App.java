package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.union.Example;
import com.pulumi.union.ExampleArgs;
import com.pulumi.union.EnumOutput;
import com.pulumi.union.EnumOutputArgs;
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
        var stringOrIntegerExample1 = new Example("stringOrIntegerExample1", ExampleArgs.builder()
            .stringOrIntegerProperty(42)
            .build());

        var stringOrIntegerExample2 = new Example("stringOrIntegerExample2", ExampleArgs.builder()
            .stringOrIntegerProperty("forty two")
            .build());

        var mapMapUnionExample = new Example("mapMapUnionExample", ExampleArgs.builder()
            .mapMapUnionProperty(Map.of("key1", Map.of("key1a", com.pulumi.union.inputs.ExampleMapMapUnionPropertyArgs.of("value1a"))))
            .build());

        ctx.export("mapMapUnionOutput", mapMapUnionExample.mapMapUnionProperty());
        // List<Union<String, Enum>> pattern
        var stringEnumUnionListExample = new Example("stringEnumUnionListExample", ExampleArgs.builder()
            .stringEnumUnionListProperty(            
                com.pulumi.union.inputs.ExampleStringEnumUnionListPropertyArgs.of("Listen"),
                com.pulumi.union.inputs.ExampleStringEnumUnionListPropertyArgs.of("Send"),
                com.pulumi.union.inputs.ExampleStringEnumUnionListPropertyArgs.of("NotAnEnumValue"))
            .build());

        // Safe enum: literal string matching an enum value
        var safeEnumExample = new Example("safeEnumExample", ExampleArgs.builder()
            .typedEnumProperty("Block")
            .build());

        // Output enum: output from another resource used as enum input
        var enumOutputExample = new EnumOutput("enumOutputExample", EnumOutputArgs.builder()
            .name("example")
            .build());

        var outputEnumExample = new Example("outputEnumExample", ExampleArgs.builder()
            .typedEnumProperty(enumOutputExample.type().applyValue(com.pulumi.union.inputs.ExampleTypedEnumPropertyArgs::of))
            .build());

    }
}
