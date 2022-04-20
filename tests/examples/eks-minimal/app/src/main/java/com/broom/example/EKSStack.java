package com.broom.example;

import com.google.gson.GsonBuilder;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.aws.iam.*;
import com.pulumi.core.Output;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



public class EKSStack {

    private final Cluster cluster;

    /**
     * Creates an EKS cluster
     * @param clusterPolicies
     */
    public EKSStack(List<Policy> clusterPolicies) {

        var vpcIdOutput = Output.of(
                Ec2Functions.getVpc(
                        GetVpcArgs.builder().default_(true).build()
                ).thenApply(GetVpcResult::id)
        );

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

        var cluster = new Cluster("my-cluster", ClusterArgs.builder()
                .vpcId(vpcIdOutput)
                .subnetIds(subnetIdsOutput)
                .instanceType("t2.micro")
                .minSize(1)
                .maxSize(2)
                .build(), ComponentResourceOptions.builder()
                .protect(true)
                .build());

        var roleName = cluster.instanceRoles()
                .applyValue(t -> t.get(0))
                .apply(Role::name);

        for (int i = 0; i < clusterPolicies.size(); i++) {
            var policy = clusterPolicies.get(i);
            new RolePolicyAttachment(String.format("cluster-role-policy-attachment-%d", i),
                    RolePolicyAttachmentArgs.builder()
                            .role(roleName)
                            .policyArn(policy.arn())
                            .build());
        }
        this.cluster = cluster;
    }

    public Output<String> kubeconfig() {
        return this.cluster.kubeconfig().applyValue(obj -> {
            final var gson = new GsonBuilder().disableHtmlEscaping().create();
            final var data = gson.toJson(obj);
            return kubeconfigJSON(data);
        });
    }

    /**
     * Helper function to convert a JSON kubeconfig to a YAML
     * @param data
     * @return EKS kubeconfig in YAML format
     */
    private static String kubeconfigJSON(String data) {
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
}
