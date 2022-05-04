package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;
import static com.pulumi.codegen.internal.Serialization.*;
import com.pulumi.codegen.internal.KeyedValue;
import static com.pulumi.codegen.internal.Files.readDir;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var siteBucket = new Bucket("siteBucket", BucketArgs.builder()        
            .website(BucketWebsiteArgs.builder()
                .indexDocument("index.html")
                .build())
            .build());

        final var siteDir = "www";

        for (var range : readDir(siteDir)) {
            new BucketObject("files-" + range.key(), BucketObjectArgs.builder()            
                .bucket(siteBucket.id())
                .key(range.value())
                .source(new FileAsset(Paths.get(siteDir, range.value())))
                .contentType(Files.probeContentType(range.value()))
                .build());
        }

        var bucketPolicy = new BucketPolicy("bucketPolicy", BucketPolicyArgs.builder()        
            .bucket(siteBucket.id())
            .policy(siteBucket.id().apply(id -> serializeJson(
                jsonObject(
                    jsonProperty("Version", "2012-10-17"),
                    jsonProperty("Statement", jsonArray(jsonObject(
                        jsonProperty("Effect", "Allow"),
                        jsonProperty("Principal", "*"),
                        jsonProperty("Action", jsonArray("s3:GetObject")),
                        jsonProperty("Resource", jsonArray(String.format("arn:aws:s3:::%s/*", id)))
                    )))
                ))))
            .build());

        ctx.export("bucketName", siteBucket.bucket());
        ctx.export("websiteUrl", siteBucket.websiteEndpoint());
    }
}
