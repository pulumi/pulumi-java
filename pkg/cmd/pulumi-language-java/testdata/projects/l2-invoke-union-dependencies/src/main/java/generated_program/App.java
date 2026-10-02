package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simpleinvoke.StringResource;
import com.pulumi.simpleinvoke.StringResourceArgs;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.SecretInvokeArgs;
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
        // Baseline for invoke dependency propagation: an invoke that reads properties from two different
        // resources produces a return value whose consumer must depend on the union of both.
        var a = new StringResource("a", StringResourceArgs.builder()
            .text("hello")
            .build());

        var b = new Resource("b", ResourceArgs.builder()
            .value(true)
            .build());

        final var data = SimpleinvokeFunctions.secretInvoke(SecretInvokeArgs.builder()
            .value(a.text())
            .secretResponse(b.value())
            .build());

        var d = new StringResource("d", StringResourceArgs.builder()
            .text(data.applyValue(_data -> _data.response()))
            .build());

    }
}
