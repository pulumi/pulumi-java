package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.enummodule.storage.Bucket;
import com.pulumi.enummodule.storage.BucketArgs;
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
        var bucket = new Bucket("bucket", BucketArgs.builder()
            .tier(com.pulumi.enummodule.storage_Tier.enums.Tier.Hot)
            .build());

    }
}
