package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
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
        ctx.export("zero", 0);
        ctx.export("one", 1);
        ctx.export("e", 2.718);
        ctx.export("minInt32", -2147483648);
        ctx.export("max", 1.7976931348623157e+308);
        ctx.export("min", 5e-324);
    }
}
