package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.awsnative.s3.Bucket;
import com.pulumi.awsnative.s3.BucketArgs;
import com.pulumi.awsnative.s3.inputs.BucketWebsiteConfigurationArgs;
import com.pulumi.aws.s3.BucketObject;
import com.pulumi.aws.s3.BucketObjectArgs;
import com.pulumi.aws.s3.BucketPolicy;
import com.pulumi.aws.s3.BucketPolicyArgs;
import com.pulumi.asset.FileAsset;
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
        var siteBucket = new Bucket("siteBucket", BucketArgs.builder()        
            .websiteConfiguration(BucketWebsiteConfigurationArgs.builder()
                .indexDocument("index.html")
                .build())
            .build());

        var indexHtml = new BucketObject("indexHtml", BucketObjectArgs.builder()        
            .bucket(siteBucket)
            .source(new FileAsset("./www/index.html"))
            .acl("public-read")
            .contentType("text/html")
            .build());

        var faviconPng = new BucketObject("faviconPng", BucketObjectArgs.builder()        
            .bucket(siteBucket)
            .source(new FileAsset("./www/favicon.png"))
            .acl("public-read")
            .contentType("image/png")
            .build());

        var bucketPolicy = new BucketPolicy("bucketPolicy", BucketPolicyArgs.builder()        
            .bucket(siteBucket.id())
            .policy(siteBucket.arn().applyValue(arn -> """
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": ["s3:GetObject"],
      "Resource": ["%s/*"]
    }
  ]
}
", arn)))
            .build());

        ctx.export("bucketName", siteBucket.bucketName());
        ctx.export("websiteUrl", siteBucket.websiteURL());
    }
}
