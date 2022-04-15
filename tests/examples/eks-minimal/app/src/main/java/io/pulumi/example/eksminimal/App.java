package com.pulumi.example.eksminimal;

import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.context.ExportContext;
import com.pulumi.context.StackContext;
import com.pulumi.core.Output;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;

import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static ExportContext stack(StackContext ctx) {
        var vpcIdOutput = Output.of(
                Ec2Functions.getVpc(
                        GetVpcArgs.builder().default_(true).build()
                ).thenApply(GetVpcResult::getId)
        );
        ctx.export("vpcIdOutput", vpcIdOutput);

        var subnetIdsOutput = vpcIdOutput
                .apply(vpcId -> Output.of(Ec2Functions.getSubnetIds(GetSubnetIdsArgs.builder()
                                .vpcId(vpcId)
                                .build())))
                .applyValue(getSubnetIdsResult ->
                        getSubnetIdsResult.getIds()
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

        ctx.export("kubeconfig", cluster.getKubeconfig());
        return ctx.exports();
    }
}
