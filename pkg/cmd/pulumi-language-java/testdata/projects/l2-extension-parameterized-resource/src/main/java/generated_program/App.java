package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.myext.Greeting;
import com.pulumi.myext.GreetingComponent;
import com.pulumi.myext.MyextFunctions;
import com.pulumi.myext.inputs.GreetArgs;
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
        var greeting = new Greeting("greeting");

        var greetingComp = new GreetingComponent("greetingComp");

        ctx.export("parameterValue", greeting.parameterValue());
        ctx.export("parameterValueFromComponent", greetingComp.parameterValue());
        ctx.export("invokeGreeting", MyextFunctions.greet(GreetArgs.builder()
            .name("Pulumi")
            .build()).applyValue(_invoke -> _invoke.greeting()));
    }
}
