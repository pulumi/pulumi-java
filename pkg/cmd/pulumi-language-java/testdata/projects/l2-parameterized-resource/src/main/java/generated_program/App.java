package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.subpackage.HelloWorld;
import com.pulumi.subpackage.HelloWorldComponent;
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
        // The resource name is based on the parameter value
        var example = new HelloWorld("example");

        var exampleComponent = new HelloWorldComponent("exampleComponent");

        ctx.export("parameterValue", example.parameterValue());
        ctx.export("parameterValueFromComponent", exampleComponent.parameterValue());
    }
}
