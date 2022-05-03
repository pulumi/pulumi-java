package eksminimal;

import io.pulumi.Stack;
import io.pulumi.aws.ec2.GetSubnetIds;
import io.pulumi.aws.ec2.GetVpc;
import io.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import io.pulumi.aws.ec2.inputs.GetVpcArgs;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.eks.Cluster;
import io.pulumi.eks.ClusterArgs;

import java.util.stream.Collectors;

public final class MyStack extends Stack {

    @Export(type = String.class)
    private Output<String> vpcIdOutput;

    @Export(type = String.class)
    private Output<String> subnetIdsOutput;

    @Export(type = Object.class)
    private Output<Object> kubeconfig;

    public MyStack() {
        var vpcIdOutput = Output.of(GetVpc.invokeAsync(GetVpcArgs.builder()
                                .$default(true)
                                .build(),
                        InvokeOptions.Empty)
                .thenApply(getVpcResult -> getVpcResult.getId()));

        this.vpcIdOutput = vpcIdOutput;

        var subnetIdsOutput = vpcIdOutput
                .apply(vpcId -> Output.of(GetSubnetIds.invokeAsync(GetSubnetIdsArgs.builder()
                                .vpcId(vpcId)
                                .build(),
                        InvokeOptions.Empty)))
                .applyValue(getSubnetIdsResult ->
                        getSubnetIdsResult.getIds()
                                .stream()
                                .sorted()
                                .limit(2)
                                .collect(Collectors.toList()));

        this.subnetIdsOutput = subnetIdsOutput.applyValue(vs -> String.join(",", vs));

        var cluster = new Cluster("my-cluster", ClusterArgs.builder()
                .vpcId(vpcIdOutput)
                .subnetIds(subnetIdsOutput)
                .instanceType("t2.micro")
                .minSize(1)
                .maxSize(2)
                .build());

        this.kubeconfig = cluster.getKubeconfig();
    }
}
