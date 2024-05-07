package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.Provider;
import com.pulumi.aws.ProviderArgs;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.s3.BucketArgs;
import com.pulumi.aws.s3.inputs.BucketWebsiteArgs;
import com.pulumi.resources.CustomResourceOptions;
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
        var provider = new Provider("provider", ProviderArgs.builder()
            .region("us-west-2")
            .build());

        var bucket = new Bucket("bucket", BucketArgs.builder()
            .website(BucketWebsiteArgs.builder()
                .indexDocument("index.html")
                .build())
            .build(), CustomResourceOptions.builder()
                .provider(provider)
                .protect(true)
                .dependsOn(provider)
                .build());

        var bucketWithoutArgs = new Bucket("bucketWithoutArgs", BucketArgs.Empty, CustomResourceOptions.builder()
            .provider(provider)
            .protect(true)
            .dependsOn(provider)
            .ignoreChanges("bucket", "lifecycleRules[0]")
            .build());

    }
}
