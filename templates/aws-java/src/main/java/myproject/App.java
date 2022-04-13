package myproject;

import io.pulumi.Pulumi;
import io.pulumi.core.Output;
import io.pulumi.aws.s3.Bucket;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(ctx -> {
            var bucket = new Bucket("my-bucket");
            ctx.export("bucketName", bucket.getId());
            return ctx.exports();
        });
        System.exit(exitCode);
    }
}
