// from https://github.com/pulumi/examples/blob/8cc8b1a4384c8b33f70ba65c701e19daecfa6399/aws-ts-s3-folder/index.ts
package s3site;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import io.pulumi.Stack;
import io.pulumi.aws.s3.BucketObject;
import io.pulumi.aws.s3.BucketPolicy;
import io.pulumi.awsnative.s3.Bucket;
import io.pulumi.awsnative.s3.inputs.BucketWebsiteConfigurationArgs;
import io.pulumi.core.Asset.FileAsset;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;
import io.pulumi.resources.CustomResourceOptions;

public final class MyStack extends Stack {

    @OutputExport(type = String.class)
    private Output<String> websiteUrl;

    @OutputExport(type = String.class)
    private Output<String> bucketName;

    @OutputExport(type = String.class)
    private Output<String> staticEndpoint;

    @OutputExport(type = String.class)
    private Output<String> cdnEndpoint;

    public MyStack() throws IOException {

        final var siteBucket = new Bucket("s3-website-bucket",
                $ -> $.websiteConfiguration(BucketWebsiteConfigurationArgs.builder()
                        .indexDocument("index.html")
                        .build()));

        final String sitedir = "www/";
        for (var path : Files.walk(Paths.get(sitedir)).filter(Files::isRegularFile).collect(Collectors.toList())) {
                var contentType = Files.probeContentType(path);
                new BucketObject(path.toString().replace(sitedir, ""),
                    $ -> $.bucket(siteBucket.getId().toInput())
                        .source(new FileAsset(path.toAbsolutePath().toString()))
                        .contentType(contentType)
                    );
        }

        final var bucketPolicy = new BucketPolicy("bucketPolicy",
                $ -> $.bucket(siteBucket.getId().toInput())
                        .policy(siteBucket.getArn().applyValue(
                                bucketArn ->
                                        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"" + bucketArn + "/*\"]}]}").toInput()
                        ));

        this.bucketName = siteBucket.getBucketName();
        this.websiteUrl = siteBucket.getWebsiteURL();
    }
}
