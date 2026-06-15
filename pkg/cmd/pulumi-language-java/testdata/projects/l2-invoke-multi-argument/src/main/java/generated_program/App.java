package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.multiargumentinvoke.MultiargumentinvokeFunctions;
import com.pulumi.multiargumentinvoke.inputs.MultiArgumentInvokeArgs;
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
        ctx.export("both", MultiargumentinvokeFunctions.multiArgumentInvoke("hello", "world").applyValue(_invoke -> _invoke.result()));
        ctx.export("onlyRequired", MultiargumentinvokeFunctions.multiArgumentInvoke("hello", null).applyValue(_invoke -> _invoke.result()));
    }
}
