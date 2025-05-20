package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.config.Provider;
import com.pulumi.config.ProviderArgs;
import com.pulumi.config.Resource;
import com.pulumi.config.ResourceArgs;
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
        var prov = new Provider("prov", ProviderArgs.builder()
            .name("my config")
            .pluginDownloadURL("not the same as the pulumi resource option")
            .build());

        // Note this isn't _using_ the explicit provider, it's just grabbing a value from it.
        var res = new Resource("res", ResourceArgs.builder()
            .text(prov.version())
            .build());

        ctx.export("pluginDownloadURL", prov.pluginDownloadURL());
    }
}
