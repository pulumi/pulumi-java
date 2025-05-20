package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.anytypefunction.AnytypefunctionFunctions;
import com.pulumi.anytypefunction.inputs.DynListToDynArgs;
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
        final var localValue = "hello";

        ctx.export("dynamic", AnytypefunctionFunctions.dynListToDyn(DynListToDynArgs.builder()
            .inputs(            
                "hello",
                localValue,
                Map.ofEntries(
                ))
            .build()).applyValue(_invoke -> _invoke.result()));
    }
}
