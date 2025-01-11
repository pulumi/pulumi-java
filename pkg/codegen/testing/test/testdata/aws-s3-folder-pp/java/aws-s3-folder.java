package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.s3.BucketArgs;
import com.pulumi.aws.s3.inputs.BucketWebsiteArgs;
import com.pulumi.aws.s3.BucketObject;
import com.pulumi.aws.s3.BucketObjectArgs;
import com.pulumi.aws.s3.BucketPolicy;
import com.pulumi.aws.s3.BucketPolicyArgs;
import com.pulumi.asset.FileAsset;
import static com.pulumi.codegen.internal.Files.readDir;
import static com.pulumi.codegen.internal.Serialization.*;
import com.pulumi.codegen.internal.KeyedValue;
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
        // Create a bucket and expose a website index document
        var siteBucket = new Bucket("siteBucket", BucketArgs.builder()
            .website(BucketWebsiteArgs.builder()
                .indexDocument("index.html")
                .build())
            .build());

        final var siteDir = "www";

        // For each file in the directory, create an S3 object stored in `siteBucket`
        for (var range : readDir(siteDir)) {
            new BucketObject("files-" + range.key(), BucketObjectArgs.builder()
                .bucket(siteBucket.id())
                .key(range.value())
                .source(new FileAsset(Paths.get(siteDir, range.value())))
                .contentType(Files.probeContentType(range.value()))
                .build());
        }

        // set the MIME type of the file
        // Set the access policy for the bucket so all objects are readable
        var bucketPolicy = new BucketPolicy("bucketPolicy", BucketPolicyArgs.builder()
            .bucket(siteBucket.id())
            .policy(siteBucket.id().applyValue(_id -> serializeJson(
                jsonObject(
                    jsonProperty("Version", "2012-10-17"),
                    jsonProperty("Statement", jsonArray(jsonObject(
                        jsonProperty("Effect", "Allow"),
                        jsonProperty("Principal", "*"),
                        jsonProperty("Action", jsonArray("s3:GetObject")),
                        jsonProperty("Resource", jsonArray(String.format("arn:aws:s3:::%s/*", _id)))
                    )))
                ))))
            .build());

        ctx.export("bucketName", siteBucket.bucket());
        ctx.export("websiteUrl", siteBucket.websiteEndpoint());
    }
}
