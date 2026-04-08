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
        // Stage 1: Change properties to trigger replacements
        // Scenario 1: Change replaceProp → REPLACE (schema triggers)
        var schemaReplace = new ResourceA("schemaReplace", ResourceAArgs.builder()
            .value(true)
            .replaceProp(false)
            .build());

        // Changed from true
        // Scenario 2: Change value → REPLACE (option triggers)
        var optionReplace = new ResourceB("optionReplace", ResourceBArgs.builder()
            .value(false)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 3: Change value → REPLACE (option on value triggers)
        var bothReplaceValue = new ResourceA("bothReplaceValue", ResourceAArgs.builder()
            .value(false)
            .replaceProp(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 4: Change replaceProp → REPLACE (schema on replaceProp triggers)
        var bothReplaceProp = new ResourceA("bothReplaceProp", ResourceAArgs.builder()
            .value(true)
            .replaceProp(false)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 5: Change value → UPDATE (no replaceOnChanges)
        var regularUpdate = new ResourceB("regularUpdate", ResourceBArgs.builder()
            .value(false)
            .build());

        // Changed from true
        // Scenario 6: No change → SAME (no operation)
        var noChange = new ResourceB("noChange", ResourceBArgs.builder()
            .value(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 7: Change replaceProp (not value) → UPDATE (marked property unchanged)
        var wrongPropChange = new ResourceA("wrongPropChange", ResourceAArgs.builder()
            .value(true)
            .replaceProp(false)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value")
                .build());

        // Scenario 8: Change value → REPLACE (multiple properties marked)
        var multiplePropReplace = new ResourceA("multiplePropReplace", ResourceAArgs.builder()
            .value(false)
            .replaceProp(true)
            .build(), CustomResourceOptions.builder()
                .replaceOnChanges("value", "replaceProp")
                .build());

    }
}
