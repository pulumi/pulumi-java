package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
        var prim = new com.pulumi.primitive.Resource("prim", com.pulumi.primitive.ResourceArgs.builder()
            .boolean_(false)
            .float_(2.17)
            .integer(-12)
            .string("Goodbye")
            .numberArray(            
                0.0,
                1.0)
            .booleanMap(Map.ofEntries(
                Map.entry("my key", false),
                Map.entry("my.key", true),
                Map.entry("my-key", false),
                Map.entry("my_key", true),
                Map.entry("MY_KEY", false),
                Map.entry("myKey", true)
            ))
            .build());

        var ref = new com.pulumi.primitiveref.Resource("ref", com.pulumi.primitiveref.ResourceArgs.builder()
            .data(com.pulumi.primitiveref.inputs.DataArgs.builder()
                .boolean_(false)
                .float_(2.17)
                .integer(-12)
                .string("Goodbye")
                .boolArray(                
                    false,
                    true)
                .stringMap(Map.ofEntries(
                    Map.entry("my key", "one"),
                    Map.entry("my.key", "two"),
                    Map.entry("my-key", "three"),
                    Map.entry("my_key", "four"),
                    Map.entry("MY_KEY", "five"),
                    Map.entry("myKey", "six")
                ))
                .build())
            .build());

        var rref = new com.pulumi.refref.Resource("rref", com.pulumi.refref.ResourceArgs.builder()
            .data(com.pulumi.refref.inputs.DataArgs.builder()
                .innerData(com.pulumi.refref.inputs.InnerDataArgs.builder()
                    .boolean_(false)
                    .float_(-2.17)
                    .integer(123)
                    .string("Goodbye")
                    .boolArray()
                    .stringMap(Map.ofEntries(
                        Map.entry("my key", "one"),
                        Map.entry("my.key", "two"),
                        Map.entry("my-key", "three"),
                        Map.entry("my_key", "four"),
                        Map.entry("MY_KEY", "five"),
                        Map.entry("myKey", "six")
                    ))
                    .build())
                .boolean_(true)
                .float_(4.5)
                .integer(1024)
                .string("Hello")
                .boolArray()
                .stringMap(Map.ofEntries(
                    Map.entry("my key", "one"),
                    Map.entry("my.key", "two"),
                    Map.entry("my-key", "three"),
                    Map.entry("my_key", "four"),
                    Map.entry("MY_KEY", "five"),
                    Map.entry("myKey", "six")
                ))
                .build())
            .build());

        var plains = new com.pulumi.plain.Resource("plains", com.pulumi.plain.ResourceArgs.builder()
            .data(com.pulumi.plain.inputs.DataArgs.builder()
                .innerData(com.pulumi.plain.inputs.InnerDataArgs.builder()
                    .boolean_(false)
                    .float_(2.17)
                    .integer(-12)
                    .string("Goodbye")
                    .boolArray(                    
                        false,
                        true)
                    .stringMap(Map.ofEntries(
                        Map.entry("my key", "one"),
                        Map.entry("my.key", "two"),
                        Map.entry("my-key", "three"),
                        Map.entry("my_key", "four"),
                        Map.entry("MY_KEY", "five"),
                        Map.entry("myKey", "six")
                    ))
                    .build())
                .boolean_(true)
                .float_(4.5)
                .integer(1024)
                .string("Hello")
                .boolArray(                
                    true,
                    false)
                .stringMap(Map.ofEntries(
                    Map.entry("my key", "one"),
                    Map.entry("my.key", "two"),
                    Map.entry("my-key", "three"),
                    Map.entry("my_key", "four"),
                    Map.entry("MY_KEY", "five"),
                    Map.entry("myKey", "six")
                ))
                .build())
            .nonPlainData(com.pulumi.plain.inputs.DataArgs.builder()
                .innerData(com.pulumi.plain.inputs.InnerDataArgs.builder()
                    .boolean_(false)
                    .float_(2.17)
                    .integer(-12)
                    .string("Goodbye")
                    .boolArray(                    
                        false,
                        true)
                    .stringMap(Map.ofEntries(
                        Map.entry("my key", "one"),
                        Map.entry("my.key", "two"),
                        Map.entry("my-key", "three"),
                        Map.entry("my_key", "four"),
                        Map.entry("MY_KEY", "five"),
                        Map.entry("myKey", "six")
                    ))
                    .build())
                .boolean_(true)
                .float_(4.5)
                .integer(1024)
                .string("Hello")
                .boolArray(                
                    true,
                    false)
                .stringMap(Map.ofEntries(
                    Map.entry("my key", "one"),
                    Map.entry("my.key", "two"),
                    Map.entry("my-key", "three"),
                    Map.entry("my_key", "four"),
                    Map.entry("MY_KEY", "five"),
                    Map.entry("myKey", "six")
                ))
                .build())
            .build());

    }
}
