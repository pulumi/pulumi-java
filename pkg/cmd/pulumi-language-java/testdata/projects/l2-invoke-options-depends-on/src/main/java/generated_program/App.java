package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simpleinvoke.Provider;
import com.pulumi.simpleinvoke.StringResource;
import com.pulumi.simpleinvoke.StringResourceArgs;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.MyInvokeArgs;
import com.pulumi.deployment.InvokeOutputOptions;
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
        var explicitProvider = new Provider("explicitProvider");

        var first = new StringResource("first", StringResourceArgs.builder()
            .text("first hello")
            .build());

        final var data = SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value("hello")
            .build(), (new InvokeOutputOptions.Builder())
                .dependsOn(first)
                .build());

        var second = new StringResource("second", StringResourceArgs.builder()
            .text(data.applyValue(_data -> _data.result()))
            .build());

        ctx.export("hello", data.applyValue(_data -> _data.result()));
    }
}
