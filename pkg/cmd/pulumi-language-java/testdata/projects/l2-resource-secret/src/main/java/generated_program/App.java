package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.secret.Resource;
import com.pulumi.secret.ResourceArgs;
import com.pulumi.secret.inputs.DataArgs;
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
        var res = new Resource("res", ResourceArgs.builder()
            .private_("closed")
            .public_("open")
            .privateData(DataArgs.builder()
                .private_("closed")
                .public_("open")
                .build())
            .publicData(DataArgs.builder()
                .private_("closed")
                .public_("open")
                .build())
            .build());

    }
}
