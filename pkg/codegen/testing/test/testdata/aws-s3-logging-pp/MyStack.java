package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

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

        ctx.export("targetBucket", bucket.loggings().apply(loggings -> loggings[0].targetBucket()));
    }
}
