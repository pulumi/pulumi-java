package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.componentpropertydeps.Custom;
import com.pulumi.componentpropertydeps.CustomArgs;
import com.pulumi.componentpropertydeps.Component;
import com.pulumi.componentpropertydeps.ComponentArgs;
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
        var custom1 = new Custom("custom1", CustomArgs.builder()
            .value("hello")
            .build());

        var custom2 = new Custom("custom2", CustomArgs.builder()
            .value("world")
            .build());

        var component1 = new Component("component1", ComponentArgs.builder()
            .resource(custom1)
            .resourceList(            
                custom1,
                custom2)
            .resourceMap(Map.ofEntries(
                Map.entry("one", custom1),
                Map.entry("two", custom2)
            ))
            .build());

        ctx.export("propertyDepsFromCall", "TODO: call call".applyValue(_call -> _call.result()));
    }
}
