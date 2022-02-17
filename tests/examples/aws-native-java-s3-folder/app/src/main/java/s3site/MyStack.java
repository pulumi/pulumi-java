// from https://github.com/pulumi/examples/blob/8cc8b1a4384c8b33f70ba65c701e19daecfa6399/aws-ts-s3-folder/index.ts
package s3site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import io.pulumi.Stack;
import io.pulumi.aws.s3.BucketObject;
import io.pulumi.aws.s3.BucketObjectArgs;
import io.pulumi.aws.s3.BucketPolicy;
import io.pulumi.aws.s3.BucketPolicyArgs;
import io.pulumi.awsnative.s3.Bucket;
import io.pulumi.awsnative.s3.BucketArgs;
import io.pulumi.awsnative.s3.inputs.BucketWebsiteConfigurationArgs;
import io.pulumi.core.Asset.FileAsset;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.resources.CustomResourceOptions;

public final class MyStack extends Stack {

    @OutputExport(name="websiteUrl", type=String.class, parameters={})
    private Output<String> websiteUrl;

    @OutputExport(name="bucketName", type=String.class, parameters={})
    private Output<String> bucketName;

    @OutputExport(name="staticEndpoint", type=String.class, parameters={})
    private Output<String> staticEndpoint;

    @OutputExport(name="cdnEndpoint", type=String.class, parameters={})
    private Output<String> cdnEndpoint;

    public MyStack() throws IOException {

        final var siteBucket = new Bucket("s3-website-bucket", BucketArgs.builder()
            .setWebsiteConfiguration(BucketWebsiteConfigurationArgs.builder()
                .setIndexDocument("index.html")
                .build())
            .build(),
            CustomResourceOptions.Empty);

        final String sitedir = "www/";
        for (var path : Files.walk(Paths.get(sitedir)).filter(Files::isRegularFile).collect(Collectors.toList())) {
            new BucketObject(path.toString().replace(sitedir, ""), 
                BucketObjectArgs.builder()
                    .setBucket(siteBucket.getId().toInput())
                    .setSource(new FileAsset(path.toAbsolutePath().toString()))
                    .setContentType(Files.probeContentType(path))
                    .build(),
                CustomResourceOptions.Empty);
        }

        final var bucketPolicy = new BucketPolicy("bucketPolicy", BucketPolicyArgs.builder()
            .setBucket(siteBucket.getId().toInput())
            .setPolicy(siteBucket.getArn().applyValue(
                bucketArn -> 
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\""+bucketArn+"/*\"]}]}").toInput()
            )
            .build(),
        CustomResourceOptions.Empty);

        this.bucketName = siteBucket.getBucketName();
        this.websiteUrl = siteBucket.getWebsiteURL();
    }
}
