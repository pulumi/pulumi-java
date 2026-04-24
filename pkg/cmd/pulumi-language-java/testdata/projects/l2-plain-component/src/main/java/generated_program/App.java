package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.plaincomponent.Component;
import com.pulumi.plaincomponent.ComponentArgs;
import com.pulumi.plaincomponent.inputs.SettingsArgs;
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
        var myComponent = new Component("myComponent", ComponentArgs.builder()
            .name("my-resource")
            .settings(SettingsArgs.builder()
                .enabled(true)
                .tags(Map.of("env", "test"))
                .build())
            .build());

        ctx.export("label", myComponent.label());
    }
}
