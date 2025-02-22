package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.component.ComponentCallable;
import com.pulumi.component.ComponentCallableArgs;
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
        var component1 = new ComponentCallable("component1", ComponentCallableArgs.builder()
            .value("bar")
            .build());

        ctx.export("from_identity", "TODO: call call".applyValue(_call -> _call.result()));
        ctx.export("from_prefixed", "TODO: call call".applyValue(_call -> _call.result()));
    }
}
