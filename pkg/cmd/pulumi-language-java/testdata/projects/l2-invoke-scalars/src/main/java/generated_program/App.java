package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.scalarreturns.ScalarreturnsFunctions;
import com.pulumi.scalarreturns.inputs.InvokeSecretArgs;
import com.pulumi.scalarreturns.inputs.InvokeArrayArgs;
import com.pulumi.scalarreturns.inputs.InvokeMapArgs;
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
        ctx.export("secret", ScalarreturnsFunctions.invokeSecret(InvokeSecretArgs.builder()
            .value("goodbye")
            .build()));
        ctx.export("array", ScalarreturnsFunctions.invokeArray(InvokeArrayArgs.builder()
            .value("the word")
            .build()));
        ctx.export("map", ScalarreturnsFunctions.invokeMap(InvokeMapArgs.builder()
            .value("hello")
            .build()));
        ctx.export("secretMap", ScalarreturnsFunctions.invokeMap(InvokeMapArgs.builder()
            .value("secret")
            .build()));
    }
}
