package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.replaceonchanges.ResourceA;
import com.pulumi.replaceonchanges.ResourceAArgs;
import com.pulumi.replaceonchanges.ResourceB;
import com.pulumi.replaceonchanges.ResourceBArgs;
import com.pulumi.resources.CustomResourceOptions;
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
        // Stage 0: Initial resource creation
        // Scenario 1: Schema-based replaceOnChanges on replaceProp
        var schemaReplace = new ResourceA("schemaReplace", ResourceAArgs.builder()
            .value(true)
            .replaceProp(true)
            .build());

        // Scenario 2: Option-based replaceOnChanges on value
        var optionReplace = new ResourceB("optionReplace", ResourceBArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 3: Both schema and option - will change value
        var bothReplaceValue = new ResourceA("bothReplaceValue", ResourceAArgs.builder()
            .value(true)
            .replaceProp(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 4: Both schema and option - will change replaceProp
        var bothReplaceProp = new ResourceA("bothReplaceProp", ResourceAArgs.builder()
            .value(true)
            .replaceProp(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 5: No replaceOnChanges - baseline update
        var regularUpdate = new ResourceB("regularUpdate", ResourceBArgs.builder()
            .value(true)
            .build());

        // Scenario 6: replaceOnChanges set but no change
        var noChange = new ResourceB("noChange", ResourceBArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 7: replaceOnChanges on value, but only replaceProp changes
        var wrongPropChange = new ResourceA("wrongPropChange", ResourceAArgs.builder()
            .value(true)
            .replaceProp(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 8: Multiple properties in replaceOnChanges array
        var multiplePropReplace = new ResourceA("multiplePropReplace", ResourceAArgs.builder()
            .value(true)
            .replaceProp(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value", "replaceProp")
                .build());

    }
}
