package myproject;

import io.pulumi.Config;
import io.pulumi.Pulumi;
import io.pulumi.core.Output;
import io.pulumi.gcp.storage.Bucket;
import io.pulumi.gcp.storage.BucketArgs;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(() -> {
            var bucket = new Bucket("my-bucket",
                                    BucketArgs.builder()
                                    .location("US")
                                    .build());
            return Map.of("bucketName", bucket.getUrl());
        });
        System.exit(exitCode);
    }
}
