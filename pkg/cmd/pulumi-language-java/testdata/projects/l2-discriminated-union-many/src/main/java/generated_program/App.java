package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.discriminatedunionmany.Example;
import com.pulumi.discriminatedunionmany.ExampleArgs;
import com.pulumi.discriminatedunionmany.inputs.Variant1Args;
import com.pulumi.discriminatedunionmany.inputs.Variant2Args;
import com.pulumi.discriminatedunionmany.inputs.Variant3Args;
import com.pulumi.discriminatedunionmany.inputs.Variant4Args;
import com.pulumi.discriminatedunionmany.inputs.Variant5Args;
import com.pulumi.discriminatedunionmany.inputs.Variant6Args;
import com.pulumi.discriminatedunionmany.inputs.Variant7Args;
import com.pulumi.discriminatedunionmany.inputs.Variant8Args;
import com.pulumi.discriminatedunionmany.inputs.Variant9Args;
import com.pulumi.discriminatedunionmany.inputs.Variant10Args;
import com.pulumi.discriminatedunionmany.SubsetExample;
import com.pulumi.discriminatedunionmany.SubsetExampleArgs;
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
            .unionOf(Variant1Args.builder()
                .payload("p1")
                .extra("e1")
                .build())
            .build());

        var example2 = new Example("example2", ExampleArgs.builder()
            .unionOf(Variant2Args.builder()
                .payload("p2")
                .extra("e2")
                .build())
            .build());

        var example3 = new Example("example3", ExampleArgs.builder()
            .unionOf(Variant3Args.builder()
                .payload("p3")
                .count(3)
                .build())
            .build());

        var example4 = new Example("example4", ExampleArgs.builder()
            .unionOf(Variant4Args.builder()
                .payload("p4")
                .enabled(true)
                .build())
            .build());

        var example5 = new Example("example5", ExampleArgs.builder()
            .unionOf(Variant5Args.builder()
                .payload("p5")
                .label("l5")
                .build())
            .build());

        var example6 = new Example("example6", ExampleArgs.builder()
            .unionOf(Variant6Args.builder()
                .payload("p6")
                .code(6)
                .build())
            .build());

        var example7 = new Example("example7", ExampleArgs.builder()
            .unionOf(Variant7Args.builder()
                .payload("p7")
                .message("m7")
                .build())
            .build());

        var example8 = new Example("example8", ExampleArgs.builder()
            .unionOf(Variant8Args.builder()
                .payload("p8")
                .size(8)
                .build())
            .build());

        var example9 = new Example("example9", ExampleArgs.builder()
            .unionOf(Variant9Args.builder()
                .payload("p9")
                .flag(false)
                .build())
            .build());

        var example10 = new Example("example10", ExampleArgs.builder()
            .unionOf(Variant10Args.builder()
                .payload("p10")
                .note("n10")
                .build())
            .build());

        // A SubsetExample's unionOf is typed as a 3-variant subset union. We should be
        // able to assign that output to an Example's unionOf, which is typed as the
        // full 10-variant union.
        var subset1 = new SubsetExample("subset1", SubsetExampleArgs.builder()
            .unionOf(Variant3Args.builder()
                .payload("sp")
                .count(33)
                .build())
            .build());

        var example11 = new Example("example11", ExampleArgs.builder()
            .unionOf(subset1.unionOf())
            .build());

    }
}
