package com.broom.example;

import com.google.gson.GsonBuilder;
import com.pulumi.Context;
import com.pulumi.Exports;
import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.aws.iam.*;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.core.Output;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;

import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class S3Stack {

    private final Policy readPolicy;
    private final Bucket bucket;

    public S3Stack() {
        this.bucket = new Bucket("jar-bucket");

        this.readPolicy = new Policy("read-bucket-object", PolicyArgs.builder()
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
