package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.CustomResourceOptions;
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
        var replacementTrigger = new com.pulumi.simple.Resource("replacementTrigger", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .replacementTrigger(Output.of("test2"))
                .build());

        var unknown = new com.pulumi.output.Resource("unknown", com.pulumi.output.ResourceArgs.builder()
            .value(2.0)
            .build());

        var unknownReplacementTrigger = new com.pulumi.simple.Resource("unknownReplacementTrigger", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .replacementTrigger(Output.of(unknown.output()))
                .build());

        var notReplacementTrigger = new com.pulumi.simple.Resource("notReplacementTrigger", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build());

        var secretReplacementTrigger = new com.pulumi.simple.Resource("secretReplacementTrigger", com.pulumi.simple.ResourceArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .replacementTrigger(Output.of(Output.ofSecret(List.of(                
                    3,
                    2,
                    1))))
                .build());

    }
}
