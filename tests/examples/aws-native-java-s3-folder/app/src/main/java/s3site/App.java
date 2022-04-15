// from https://github.com/pulumi/examples/blob/8cc8b1a4384c8b33f70ba65c701e19daecfa6399/aws-ts-s3-folder/index.ts
package s3site;

import io.pulumi.Context;
import io.pulumi.Exports;
import io.pulumi.Pulumi;
import io.pulumi.asset.FileAsset;
import io.pulumi.aws.s3.BucketObject;
import io.pulumi.aws.s3.BucketObjectArgs;
import io.pulumi.aws.s3.BucketPolicy;
import io.pulumi.aws.s3.BucketPolicyArgs;
import io.pulumi.awsnative.s3.Bucket;
import io.pulumi.awsnative.s3.BucketArgs;
import io.pulumi.awsnative.s3.inputs.BucketWebsiteConfigurationArgs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static Exports stack(Context ctx) {

        final var siteBucket = new Bucket("s3-website-bucket",
                BucketArgs.builder().websiteConfiguration(BucketWebsiteConfigurationArgs.builder()
                        .indexDocument("index.html")
                        .build()).build());

        final String siteDir = "www/";
        try {
            for (var path : Files.walk(Paths.get(siteDir)).filter(Files::isRegularFile).toList()) {
                var contentType = Files.probeContentType(path);
                new BucketObject(path.toString().replace(siteDir, ""),
                        BucketObjectArgs.builder().bucket(siteBucket.getId())
                                .source(new FileAsset(path.toAbsolutePath().toString()))
                                .contentType(contentType).build()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final var bucketPolicy = new BucketPolicy("bucketPolicy",
                BucketPolicyArgs.builder().bucket(siteBucket.getId())
                        .policy(siteBucket.getArn()
                                .applyValue(bucketArn -> """
                                            {
                                                "Version":"2012-10-17",
                                                "Statement":[{
                                                    "Effect":"Allow",
                                                    "Principal":"*",
                                                    "Action":["s3:GetObject"],
                                                    "Resource":["%s/*"]
                                                }]
                                            }
                                        """.formatted(bucketArn))
                        ).build());

        ctx.export("bucketName", siteBucket.getBucketName());
        ctx.export("websiteUrl", siteBucket.getWebsiteURL());

        return ctx.exports();
    }
}
