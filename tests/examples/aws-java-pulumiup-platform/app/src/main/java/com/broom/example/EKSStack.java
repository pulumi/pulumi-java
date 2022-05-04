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
import com.pulumi.resources.ComponentResourceOptions;

import java.util.List;
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

        var cluster = new Cluster("my-cluster",
                ClusterArgs.builder()
                    .vpcId(vpcIdOutput)
                    .subnetIds(subnetIdsOutput)
                    .instanceType("t2.micro")
                    .minSize(1)
                    .maxSize(2)
                    .build(),
                ComponentResourceOptions.builder()
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
            return data;
        });
    }
}
