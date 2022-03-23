package webserver;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;
import io.pulumi.aws.ec2.*;
import io.pulumi.aws.ec2.inputs.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public final class MyStack extends Stack {
    @Export(type = String.class)
    private final Output<String> publicIp;

    @Export(type = String.class)
    private final Output<String> publicHostName;

    public MyStack() throws InterruptedException, ExecutionException {
        
        final var ami = GetAmi.invokeAsync(GetAmiArgs.builder()
            .filters(new GetAmiFilter("name", List.of("amzn-ami-hvm-*-x86_64-ebs")))
                .owners("137112412989")
                .mostRecent(true).build()
        , null).thenApply(fn -> fn.getId());

        final var group = new SecurityGroup("web-secgrp", SecurityGroupArgs.builder()
            .ingress(SecurityGroupIngressArgs.builder()
                .protocol("tcp")
                    .fromPort(80)
                    .toPort(80)
                    .cidrBlocks("0.0.0.0/0")
                    .build())
            .build()
        );

        // (optional) create a simple web server using the startup
        // script for the instance

        final var userData =
            "#!/bin/bash\n"+
            "echo \"Hello, World!\" > index.html\n"+
            "nohup python -m SimpleHTTPServer 80 &";

        final var server = new Instance("web-server-www", InstanceArgs.builder()
                .tags(Map.of("Name", "web-server-www"))
                .instanceType(Output.ofRight(io.pulumi.aws.ec2.enums.InstanceType.T2_Micro))
                .vpcSecurityGroupIds(group.getId().applyValue(List::of))
                .ami(Output.of(ami))
                .userData(userData)
                .build()
        );

        this.publicIp = server.getPublicIp();
        this.publicHostName = server.getPublicDns();
    }
}
