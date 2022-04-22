package com.broom.example;

import com.pulumi.aws.iam.*;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.core.Output;

public class S3Stack {

    private final Policy readPolicy;
    private final Bucket bucket;

    public S3Stack() {
        this.bucket = new Bucket("jar-bucket");

        this.readPolicy = new Policy("read-bucket-object",
                PolicyArgs.builder()
                .policy(Output.format(
                        "{\"Version\": \"2012-10-17\", \"Statement\": [ {\"Effect\": \"Allow\", \"Action\": \"s3:GetObject\", \"Resource\": \"arn:aws:s3:::%s/*\"}]}",
                        bucket.bucket()))
                .build());
    }
    public Output<String> bucketName() {
        return this.bucket.bucket();
    }
    public Policy readPolicy() {
        return this.readPolicy;
    }
}
