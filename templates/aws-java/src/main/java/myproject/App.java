package myproject;

import io.pulumi.Config;
import io.pulumi.Pulumi;
import io.pulumi.core.Output;
import io.pulumi.aws.s3.Bucket;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(() -> {
            var bucket = new Bucket("my-bucket");
            return Map.of("bucketName", bucket.getId());
        });
        System.exit(exitCode);
    }
}
