package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.hipackage.HelloWorld;
import com.pulumi.hipackage.HelloWorldComponent;
import com.pulumi.byepackage.GoodbyeWorld;
import com.pulumi.byepackage.GoodbyeWorldComponent;
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
        var example1 = new HelloWorld("example1");

        var exampleComponent1 = new HelloWorldComponent("exampleComponent1");

        ctx.export("parameterValue1", example1.parameterValue());
        ctx.export("parameterValueFromComponent1", exampleComponent1.parameterValue());
        // The resource name is based on the parameter value
        var example2 = new GoodbyeWorld("example2");

        var exampleComponent2 = new GoodbyeWorldComponent("exampleComponent2");

        ctx.export("parameterValue2", example2.parameterValue());
        ctx.export("parameterValueFromComponent2", exampleComponent2.parameterValue());
    }
}
