package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.names.ResMap;
import com.pulumi.names.ResMapArgs;
import com.pulumi.names.ResArray;
import com.pulumi.names.ResArrayArgs;
import com.pulumi.names.ResList;
import com.pulumi.names.ResListArgs;
import com.pulumi.names.ResResource;
import com.pulumi.names.ResResourceArgs;
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
        var res1 = new ResMap("res1", ResMapArgs.builder()
            .value(true)
            .build());

        var res2 = new ResArray("res2", ResArrayArgs.builder()
            .value(true)
            .build());

        var res3 = new ResList("res3", ResListArgs.builder()
            .value(true)
            .build());

        var res4 = new ResResource("res4", ResResourceArgs.builder()
            .value(true)
            .build());

    }
}
