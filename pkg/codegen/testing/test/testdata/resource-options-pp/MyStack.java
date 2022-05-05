package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;
import com.pulumi.resources.CustomResourceOptions;

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
            .ignoreChanges(            
                bucket,
                lifecycleRules[0])
            .build());

    }
}
