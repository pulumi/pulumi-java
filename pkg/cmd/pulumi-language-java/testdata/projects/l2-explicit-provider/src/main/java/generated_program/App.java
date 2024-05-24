package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.simple.Provider;
import com.pulumi.simple.ProviderArgs;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        final var prov = new Provider("prov");

        final var res = new Resource(
            "res",
            ResourceArgs.builder().value(true).build(),
            CustomResourceOptions.builder().provider(prov).build()
        );
    }
}
