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
        /*
        // Copyright 2016-2021, Pulumi Corporation.

        import * as aws from "@pulumi/aws";
        import * as awsnative from "@pulumi/aws-native";
        import * as pulumi from "@pulumi/pulumi";
        import * as fs from "fs";
        import * as mime from "mime";

        // Create a bucket and expose a website index document
        const siteBucket = new awsnative.s3.Bucket("s3-website-bucket", {
            websiteConfiguration: {
                indexDocument: "index.html",
            },
        });
        */
        final var siteBucket = new Bucket("s3-website-bucket", BucketArgs.builder()
            .setWebsiteConfiguration(BucketWebsiteConfigurationArgs.builder()
                .setIndexDocument("index.html")
                .build())
            .build(),
            CustomResourceOptions.Empty);
        /*

        const siteDir = "www"; // directory for content files

        // For each file in the directory, create an S3 object stored in `siteBucket`
        for (const item of fs.readdirSync(siteDir)) {
            const filePath = require("path").join(siteDir, item);
            const siteObject = new aws.s3.BucketObject(item, {
                bucket: siteBucket.id,                            // reference the s3.Bucket object
                source: new pulumi.asset.FileAsset(filePath),     // use FileAsset to point to a file
                contentType: mime.getType(filePath) || undefined, // set the MIME type of the file
            });
        }
        */
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

        // Set the access policy for the bucket so all objects are readable
        /*
        const bucketPolicy = new aws.s3.BucketPolicy("bucketPolicy", {
            bucket: siteBucket.id, // refer to the bucket created earlier
            policy: siteBucket.arn.apply(bucketArn => JSON.stringify({
                Version: "2012-10-17",
                Statement: [{
                    Effect: "Allow",
                    Principal: "*",
                    Action: [
                        "s3:GetObject",
                    ],
                    Resource: [
                        `${bucketArn}/*`, //
                    ],
                }],
            })),
        });
        */
        final var bucketPolicy = new BucketPolicy("bucketPolicy", BucketPolicyArgs.builder()
            .setBucket(siteBucket.getId().toInput())
            .setPolicy(siteBucket.getArn().applyValue(
                bucketArn -> 
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\""+bucketArn+"/*\"]}]}").toInput()
            )
            .build(),
        CustomResourceOptions.Empty);
        /*

        // Stack exports
        export const bucketName = siteBucket.bucketName;
        export const websiteUrl = siteBucket.websiteURL;
        */
        this.bucketName = siteBucket.getBucketName();
        this.websiteUrl = siteBucket.getWebsiteURL();
    }
}
