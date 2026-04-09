package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.nestedobject.Receiver;
import com.pulumi.nestedobject.ReceiverArgs;
import com.pulumi.nestedobject.inputs.DetailArgs;
import com.pulumi.nestedobject.MapContainer;
import com.pulumi.nestedobject.MapContainerArgs;
import com.pulumi.nestedobject.Target;
import com.pulumi.nestedobject.TargetArgs;
import com.pulumi.resources.CustomResourceOptions;
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
        var receiverIgnore = new Receiver("receiverIgnore", ReceiverArgs.builder()
            .details(DetailArgs.builder()
                .key("a")
                .value("b")
                .build())
            .build(), CustomResourceOptions.builder()
                .ignoreChanges("details[0].key")
                .build());

        var mapIgnore = new MapContainer("mapIgnore", MapContainerArgs.builder()
            .tags(Map.of("env", "prod"))
            .build(), CustomResourceOptions.builder()
                .ignoreChanges("tags[\"env\"]", "tags[\"with.dot\"]", "tags[\"with escaped \\\"\"]")
                .build());

        var noIgnore = new Target("noIgnore", TargetArgs.builder()
            .name("nothing")
            .build());

    }
}
