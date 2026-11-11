package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.outputvalueinputdeps.Custom;
import com.pulumi.outputvalueinputdeps.CustomArgs;
import com.pulumi.outputvalueinputdeps.Component;
import com.pulumi.outputvalueinputdeps.ComponentArgs;
import com.pulumi.outputvalueinputdeps.inputs.RefsArgs;
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
        var a = new Custom("a", CustomArgs.builder()
            .value("a")
            .build());

        var b = new Custom("b", CustomArgs.builder()
            .value("b")
            .build());

        var comp = new Component("comp", ComponentArgs.builder()
            .refs(RefsArgs.builder()
                .x(a.value())
                .y(b.value())
                .build())
            .build());

    }
}
