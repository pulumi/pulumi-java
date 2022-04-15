package myproject;

import io.pulumi.Pulumi;
import io.pulumi.core.Output;
import io.pulumi.gcp.storage.Bucket;
import io.pulumi.gcp.storage.BucketArgs;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(ctx -> {
            var bucket = new Bucket("my-bucket",
                                    BucketArgs.builder()
                                    .location("US")
                                    .build());
            ctx.export("bucketName", bucket.url());
            return ctx.exports();
        });
        System.exit(exitCode);
    }
}
