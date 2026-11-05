package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simpleinvoke.Provider;
import com.pulumi.simpleinvoke.StringResource;
import com.pulumi.simpleinvoke.StringResourceArgs;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.MyInvokeArgs;
import com.pulumi.deployment.InvokeOptions;
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
        var provider = new Provider("provider");

        var parent = new StringResource("parent", StringResourceArgs.builder()
            .text("parent")
            .build(), CustomResourceOptions.builder()
                .provider(provider)
                .build());

        // This should inherit the explicit provider from parent.
        var child = new StringResource("child", StringResourceArgs.builder()
            .text("child")
            .build(), CustomResourceOptions.builder()
                .parent(parent)
                .build());

        // This should use the explicit provider of parent.
        final var parentData = SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value("parent")
            .build(), InvokeOptions.builder()
                .parent(parent)
                .build());

        // This should use the explicit provider that child inherited from parent.
        final var childData = SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value("child")
            .build(), InvokeOptions.builder()
                .parent(child)
                .build());

        ctx.export("parentResult", parentData.applyValue(_parentData -> _parentData.result()));
        ctx.export("childResult", childData.applyValue(_childData -> _childData.result()));
    }
}
