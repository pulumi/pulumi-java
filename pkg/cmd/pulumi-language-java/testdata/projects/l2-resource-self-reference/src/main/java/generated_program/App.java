package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.selfref.Node;
import com.pulumi.selfref.NodeArgs;
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
        var root = new Node("root");

        var child = new Node("child", NodeArgs.builder()
            .parent(root)
            .parents(root)
            .namedParents(Map.of("root", root))
            .parentOrName(root)
            .build());

    }
}
