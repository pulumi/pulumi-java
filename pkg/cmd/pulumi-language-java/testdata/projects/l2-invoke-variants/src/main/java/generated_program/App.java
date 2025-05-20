package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simpleinvoke.StringResource;
import com.pulumi.simpleinvoke.StringResourceArgs;
import com.pulumi.simpleinvoke.SimpleinvokeFunctions;
import com.pulumi.simpleinvoke.inputs.MyInvokeArgs;
import com.pulumi.simpleinvoke.inputs.UnitArgs;
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
        var res = new StringResource("res", StringResourceArgs.builder()
            .text("hello")
            .build());

        ctx.export("outputInput", SimpleinvokeFunctions.myInvoke(MyInvokeArgs.builder()
            .value(res.text())
            .build()).applyValue(_invoke -> _invoke.result()));
        ctx.export("unit", SimpleinvokeFunctions.unit(UnitArgs.builder()
            .build()).applyValue(_invoke -> _invoke.result()));
    }
}
