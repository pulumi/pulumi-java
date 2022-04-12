package io.pulumi.example.eksminimal;

import io.pulumi.Pulumi;
import io.pulumi.aws.ec2.Ec2Functions;
import io.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import io.pulumi.aws.ec2.inputs.GetVpcArgs;
import io.pulumi.aws.ec2.outputs.GetVpcResult;
import io.pulumi.context.ExportContext;
import io.pulumi.context.StackContext;
import io.pulumi.core.Output;
import io.pulumi.eks.Cluster;
import io.pulumi.eks.ClusterArgs;

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
