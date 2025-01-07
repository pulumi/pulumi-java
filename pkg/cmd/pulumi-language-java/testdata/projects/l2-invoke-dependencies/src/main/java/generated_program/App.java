package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.SecretInvokeArgs;
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
        var first = new Resource("first", ResourceArgs.builder()
            .value(false)
            .build());

        // assert that resource second depends on resource first
        // because it uses .secret from the invoke which depends on first
        var second = new Resource("second", ResourceArgs.builder()
            .value(SimpleinvokeFunctions.secretInvoke(SecretInvokeArgs.builder()
                .value("hello")
                .secretResponse(first.value())
                .build()).applyValue(invoke -> invoke.secret()))
            .build());

    }
}
