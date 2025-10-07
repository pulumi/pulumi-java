package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.camelNames.CoolModule.SomeResource;
import com.pulumi.camelNames.CoolModule.SomeResourceArgs;
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
        var firstResource = new SomeResource("firstResource", SomeResourceArgs.builder()
            .theInput(true)
            .build());

        var secondResource = new SomeResource("secondResource", SomeResourceArgs.builder()
            .theInput(firstResource.theOutput())
            .build());

    }
}
