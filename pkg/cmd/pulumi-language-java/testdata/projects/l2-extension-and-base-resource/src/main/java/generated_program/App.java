package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.myext.Greeting;
import com.pulumi.extbase.Base;
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

        var base = new Base("base");

        ctx.export("parameterValue", greeting.parameterValue());
        ctx.export("baseValue", base.baseValue());
    }
}
