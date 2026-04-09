package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.keywords.Lambda;
import com.pulumi.keywords.LambdaArgs;
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
        var firstResource = new com.pulumi.keywords.SomeResource("firstResource", com.pulumi.keywords.SomeResourceArgs.builder()
            .builtins("builtins")
            .lambda("lambda")
            .property("property")
            .build());

        var secondResource = new com.pulumi.keywords.SomeResource("secondResource", com.pulumi.keywords.SomeResourceArgs.builder()
            .builtins(firstResource.builtins())
            .lambda(firstResource.lambda())
            .property(firstResource.property())
            .build());

        var lambdaModuleResource = new com.pulumi.keywords.lambda.SomeResource("lambdaModuleResource", com.pulumi.keywords.lambda.SomeResourceArgs.builder()
            .builtins("builtins")
            .lambda("lambda")
            .property("property")
            .build());

        var lambdaResource = new Lambda("lambdaResource", LambdaArgs.builder()
            .builtins("builtins")
            .lambda("lambda")
            .property("property")
            .build());

    }
}
