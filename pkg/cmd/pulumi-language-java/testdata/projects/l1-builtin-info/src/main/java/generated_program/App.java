package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.deployment.Deployment;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        ctx.export("stackOutput", Output.of(Deployment.getInstance().getStackName()));

        ctx
            .export("projectOutput", Output.of(Deployment.getInstance().getProjectName()));

        ctx
            .export(
                "organizationOutput",
                Output.of(Deployment.getInstance().getOrganizationName())
            );
    }
}
