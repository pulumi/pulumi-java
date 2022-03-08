package webserver;

import io.pulumi.Stack;
import io.pulumi.core.Input;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.aws.ec2.GetAmi;
import io.pulumi.aws.ec2.Instance;
import io.pulumi.aws.ec2.inputs.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public final class MyStack extends Stack {
    @OutputExport(type = String.class)
    private final Output<String> publicIp;

    @OutputExport(type = String.class)
    private final Output<String> publicHostName;

    public MyStack() throws InterruptedException, ExecutionException {
        final var ami = GetAmi.invokeAsync($ ->
        {
            $.setFilters(List.of(new GetAmiFilter("name", List.of("amzn-ami-hvm-*-x86_64-ebs"))))
                .setOwners(List.of("137112412989"))
                .setMostRecent(true);
        }, null).thenApply(fn -> fn.getId());

        final var group = new io.pulumi.aws.ec2.SecurityGroup("web-secgrp", $ ->
        {
            var ingress = new io.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs.Builder();
            ingress.setProtocol("tcp")
                .setFromPort(80)
                .setToPort(80)
                .setCidrBlocks(List.of("0.0.0.0/0"));
            $.setIngress(List.of(ingress.build()));
        });

        // (optional) create a simple web server using the startup
        // script for the instance

        final var userData =
            "#!/bin/bash\n"+
            "echo \"Hello, World!\" > index.html\n"+
            "nohup python -m SimpleHTTPServer 80 &";

        final var server = new Instance("web-server-www", $ -> {
                $.setTags(Map.of("Name", "web-server-www"))
                    .setInstanceType(Input.ofRight(io.pulumi.aws.ec2.enums.InstanceType.T2_Micro))
                    .setVpcSecurityGroupIds(group.getId().applyValue(List::of).toInput())
                    .setAmi(Input.of(ami))
                    .setUserData(userData);
        });

        this.publicIp = server.getPublicIp();
        this.publicHostName = server.getPublicDns();
    }
}
