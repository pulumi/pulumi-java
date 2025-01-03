package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.goodbye.Provider;
import com.pulumi.goodbye.ProviderArgs;
import com.pulumi.goodbye.Goodbye;
import com.pulumi.goodbye.GoodbyeArgs;
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
        var prov = new Provider("prov", ProviderArgs.builder()
            .text("World")
            .build());

        // The resource name is based on the parameter value
        var res = new Goodbye("res", GoodbyeArgs.Empty, CustomResourceOptions.builder()
            .provider(prov)
            .build());

        ctx.export("parameterValue", res.parameterValue());
    }
}
