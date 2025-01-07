package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.MyInvokeArgs;
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
        ctx.export("hello", SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value("hello")
            .build()).applyValue(invoke -> invoke.result()));
        ctx.export("goodbye", SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value("goodbye")
            .build()).applyValue(invoke -> invoke.result()));
    }
}
