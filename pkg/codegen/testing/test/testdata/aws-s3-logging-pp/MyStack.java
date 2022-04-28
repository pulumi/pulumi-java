package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static Exports stack(Context ctx) {
        var logs = new Bucket("logs");

        var bucket = new Bucket("bucket", BucketArgs.builder()        
            .loggings(BucketLogging.builder()
                .targetBucket(logs.getBucket())
                .build())
            .build());

        ctx.export("targetBucket", bucket.getLoggings().apply(loggings -> loggings[0].getTargetBucket()));
        return ctx.exports();
    }
}
