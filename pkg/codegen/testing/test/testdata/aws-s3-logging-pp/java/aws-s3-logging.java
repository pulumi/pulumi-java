package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.s3.BucketArgs;
import com.pulumi.aws.s3.inputs.BucketLoggingArgs;
import com.pulumi.aws.s3.BucketObject;
import com.pulumi.aws.s3.BucketObjectArgs;
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
        var logs = new Bucket("logs");

        var bucket = new Bucket("bucket", BucketArgs.builder()
            .loggings(BucketLoggingArgs.builder()
                .targetBucket(logs.bucket())
                .build())
            .build());

        var indexFile = new BucketObject("indexFile", BucketObjectArgs.builder()
            .bucket(bucket.id())
            .source(Files.readString(Paths.get("./index.html")))
            .build());

        ctx.export("targetBucket", bucket.loggings().applyValue(loggings -> loggings[0].targetBucket()));
    }
}
