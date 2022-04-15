package webserver;

import io.pulumi.Context;
import io.pulumi.Exports;
import io.pulumi.Pulumi;
import io.pulumi.aws.ec2.Ec2Functions;
import io.pulumi.aws.ec2.Instance;
import io.pulumi.aws.ec2.InstanceArgs;
import io.pulumi.aws.ec2.SecurityGroup;
import io.pulumi.aws.ec2.SecurityGroupArgs;
import io.pulumi.aws.ec2.inputs.GetAmiArgs;
import io.pulumi.aws.ec2.inputs.GetAmiFilter;
import io.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import io.pulumi.aws.ec2.outputs.GetAmiResult;
import io.pulumi.core.Output;

import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    public static Exports stack(Context ctx) {
        final var ami = Ec2Functions.getAmi(GetAmiArgs.builder()
                .filters(new GetAmiFilter("name", List.of("amzn-ami-hvm-*-x86_64-ebs")))
                .owners("137112412989")
                .mostRecent(true)
                .build()
        ).thenApply(GetAmiResult::getId);

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
                "#!/bin/bash\n" +
                        "echo \"Hello, World!\" > index.html\n" +
                        "nohup python -m SimpleHTTPServer 80 &";

        final var server = new Instance("web-server-www", InstanceArgs.builder()
                .tags(Map.of("Name", "web-server-www"))
                .instanceType(Output.ofRight(io.pulumi.aws.ec2.enums.InstanceType.T2_Micro))
                .vpcSecurityGroupIds(group.getId().applyValue(List::of))
                .ami(Output.of(ami))
                .userData(userData)
                .build()
        );

        ctx.export("publicIp", server.getPublicIp());
        ctx.export("publicHostName", server.getPublicDns());
        return ctx.exports();
    }
}
