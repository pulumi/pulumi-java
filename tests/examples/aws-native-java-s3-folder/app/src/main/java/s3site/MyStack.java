// from https://github.com/pulumi/examples/blob/8cc8b1a4384c8b33f70ba65c701e19daecfa6399/aws-ts-s3-folder/index.ts
package s3site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import io.pulumi.Stack;
import io.pulumi.aws.s3.*;
import io.pulumi.awsnative.s3.Bucket;
import io.pulumi.awsnative.s3.BucketArgs;
import io.pulumi.awsnative.s3.inputs.BucketWebsiteConfigurationArgs;
import io.pulumi.core.Asset.FileAsset;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;

public final class MyStack extends Stack {

    @Export(type = String.class)
    private Output<String> websiteUrl;

    @Export(type = String.class)
    private Output<String> bucketName;

    @Export(type = String.class)
    private Output<String> staticEndpoint;

    @Export(type = String.class)
    private Output<String> cdnEndpoint;

    public MyStack() throws IOException {

        final var siteBucket = new Bucket("s3-website-bucket",
                BucketArgs.builder().websiteConfiguration(BucketWebsiteConfigurationArgs.builder()
                        .indexDocument("index.html")
                        .build()).build());

        final String sitedir = "www/";
        for (var path : Files.walk(Paths.get(sitedir)).filter(Files::isRegularFile).collect(Collectors.toList())) {
                var contentType = Files.probeContentType(path);
                new BucketObject(path.toString().replace(sitedir, ""),
                    BucketObjectArgs.builder().bucket(siteBucket.getId())
                        .source(new FileAsset(path.toAbsolutePath().toString()))
                        .contentType(contentType).build()
                    );
        }

        final var bucketPolicy = new BucketPolicy("bucketPolicy",
                BucketPolicyArgs.builder().bucket(siteBucket.getId())
                        .policy(siteBucket.getArn()
                                .applyValue(bucketArn ->
                                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"" +
                                    bucketArn +
                                    "/*\"]}]}")
                        ).build());

        this.bucketName = siteBucket.getBucketName();
        this.websiteUrl = siteBucket.getWebsiteURL();
    }
}
