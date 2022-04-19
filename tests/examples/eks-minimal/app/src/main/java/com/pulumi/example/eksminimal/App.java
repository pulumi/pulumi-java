package com.pulumi.example.eksminimal;

import com.google.gson.GsonBuilder;
import com.pulumi.Context;
import com.pulumi.Exports;
import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.aws.iam.Policy;
import com.pulumi.aws.iam.PolicyArgs;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicyAttachment;
import com.pulumi.aws.iam.RolePolicyAttachmentArgs;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.core.Output;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;

import java.text.MessageFormat;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static String toYaml(Object obj) {
        // parse JSON
        final var gson = new GsonBuilder().disableHtmlEscaping().create();
        final var data = gson.toJson(obj);

        final var endpoint = data.split(Pattern.quote("\"server\":{\"value\":\""))[1].split(Pattern.quote("\"}"))[0];
        final var caCert = data.split(Pattern.quote("\"certificate-authority-data\":{\"value\":\""))[1].split(Pattern.quote("\"}"))[0];
        final var clusterName = data.split(Pattern.quote("{\"value\":\"--cluster-name\"},{\"value\":\""))[1].split(Pattern.quote("\"}"))[0];

        return MessageFormat.format(String.join("\n",
                "apiVersion: v1",
                "clusters:",
                "- cluster:",
                "    certificate-authority-data: {0}",
                "    server: {1}",
                "  name: kubernetes",
                "contexts:",
                "- context:",
                "    cluster: kubernetes",
                "    user: aws",
                "  name: aws",
                "current-context: aws",
                "kind: Config",
                "users:",
                "- name: aws",
                "  user:",
                "    exec:",
                "      apiVersion: client.authentication.k8s.io/v1alpha1",
                "      args:",
                "      - eks",
                "      - get-token",
                "      - --cluster-name",
                "      - {2}",
                "      command: aws"
        ), caCert, endpoint, clusterName);
    }

    private static Exports stack(Context ctx) {
        final var bucket = new Bucket("jar-bucket");
        ctx.export("jarBucket", bucket.bucket());

        final var readPolicy = new Policy("read-bucket-object", PolicyArgs.builder()
                .policy(Output.format(
                        "{\"Version\": \"2012-10-17\", \"Statement\": [ {\"Effect\": \"Allow\", \"Action\": \"s3:GetObject\", \"Resource\": \"arn:aws:s3:::%s/*\"}]}",
                        bucket.bucket()))
                .build());

        var vpcIdOutput = Output.of(
                Ec2Functions.getVpc(
                        GetVpcArgs.builder().default_(true).build()
                ).thenApply(GetVpcResult::id)
        );
        ctx.export("vpcIdOutput", vpcIdOutput);

        var subnetIdsOutput = vpcIdOutput
                .apply(vpcId -> Output.of(Ec2Functions.getSubnetIds(GetSubnetIdsArgs.builder()
                                .vpcId(vpcId)
                                .build())))
                .applyValue(getSubnetIdsResult ->
                        getSubnetIdsResult.ids()
                                .stream()
                                .sorted()
                                .limit(2)
                                .collect(Collectors.toList()));

        ctx.export("subnetIdsOutput", subnetIdsOutput.applyValue(vs -> String.join(",", vs)));

        var cluster = new Cluster("my-cluster", ClusterArgs.builder()
                .vpcId(vpcIdOutput)
                .subnetIds(subnetIdsOutput)
                .instanceType("t2.micro")
                .minSize(1)
                .maxSize(2)
                .build());

        var roleName = cluster.instanceRoles()
                .applyValue(t -> t.get(0))
                .apply(Role::name);
        new RolePolicyAttachment("jar-read", RolePolicyAttachmentArgs.builder()
                .role(roleName)
                .policyArn(readPolicy.arn())
                .build());

        ctx.export("kubeconfig", cluster.kubeconfig().applyValue(data -> {
            return toYaml(data);
        }));
        return ctx.exports();
    }
}
