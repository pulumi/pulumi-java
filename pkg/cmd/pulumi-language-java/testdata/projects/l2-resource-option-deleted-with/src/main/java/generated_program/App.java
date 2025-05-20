package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.simple.Resource;
import com.pulumi.simple.ResourceArgs;
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
        var target = new Resource("target", ResourceArgs.builder()
            .value(true)
            .build());

        var deletedWith = new Resource("deletedWith", ResourceArgs.builder()
            .value(true)
            .build());

        var notDeletedWith = new Resource("notDeletedWith", ResourceArgs.builder()
            .value(true)
            .build());

    }
}
