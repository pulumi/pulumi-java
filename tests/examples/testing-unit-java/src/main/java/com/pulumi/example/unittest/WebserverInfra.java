package com.pulumi.example.unittest;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.core.Output;

import java.util.Map;

public class WebserverInfra {
    public static void main(String[] args) {
        Pulumi.run(WebserverInfra::stack);
    }

    // A simple stack to be tested.
    static void stack(Context ctx) {
        var group = new SecurityGroup("web-secgrp", SecurityGroupArgs.builder()
                .ingress(SecurityGroupIngressArgs.builder()
                        .protocol("tcp")
                        // Uncomment to fail a test:
                        // .fromPort(22)
                        // .toPort(22)
                        .fromPort(80)
                        .toPort(80)
                        .cidrBlocks("0.0.0.0/0")
                        .build()
                )
                .build()
        );

        var server = new Instance("web-server-www", InstanceArgs.builder()
                .instanceType("t2.micro")
                .vpcSecurityGroupIds(Output.all(group.name())) // reference the group object above
                .ami("ami-c55673a0") // AMI for us-east-2 (Ohio)
                // Comment out to fail a test:
                .tags(Map.of("Name", "webserver"))
                // Uncomment to fail a test:
                //.userData( /* start a simple webserver */
                //        "#!/bin/bash echo \"Hello, World!\" > index.html nohup python -m SimpleHTTPServer 80 &"
                //)
                .build()
        );
    }
}
