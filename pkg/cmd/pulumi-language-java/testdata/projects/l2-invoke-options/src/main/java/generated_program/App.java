package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simpleinvoke.Provider;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.MyInvokeArgs;
import com.pulumi.deployment.InvokeOptions;
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

        final var data = SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value("hello")
            .build(), InvokeOptions.builder()
                .provider(explicitProvider)
                .parent(explicitProvider)
                .version("10.0.0")
                .pluginDownloadURL("https://example.com/github/example")
                .build());

        ctx.export("hello", data.applyValue(_data -> _data.result()));
    }
}
