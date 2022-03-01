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

        /*
         * @OutputExport(type = String.class)
         * private final Output<String> randomPassword;
         * 
         * // TODO this does not seem to be showing up in stack outputs.
         * 
         * @OutputExport(type = Map.class, parameters = {String.class, Object.class})
         * private final Output<Map<String, Object>> randomPetKeepers;
         * 
         * @OutputExport(type = Integer.class)
         * private final Output<Integer> randomInteger;
         */
        @OutputExport(type = String.class)
        private final Output<String> publicIp;

        @OutputExport(type = String.class)
        private final Output<String> publicHostName;

        public MyStack() throws InterruptedException, ExecutionException {
            /*
             * // Copyright 2016-2021, Pulumi Corporation. All rights reserved.
             * 
             * import * as aws from "@pulumi/aws";
             * import * as pulumi from "@pulumi/pulumi";
             * 
             * // Get the id for the latest Amazon Linux AMI
             * const ami = aws.ec2.getAmi({
             * filters: [
             * { name: "name", values: ["amzn-ami-hvm-*-x86_64-ebs"] },
             * ],
             * owners: ["137112412989"], // Amazon
             * mostRecent: true,
             * }).then(result => result.id);
             */
            final var ami = GetAmi.invokeAsync($ -> {
                $.setFilters(List.of(
                    new GetAmiFilter("name", List.of("amzn-ami-hvm-*-x86_64-ebs"))
                ))
                .setOwners(List.of("137112412989"))
                .setMostRecent(true);
            }, null).thenApply(fn -> {
                return fn.getId();
            });
            /*
             * 
             * // create a new security group for port 80
             * const group = new aws.ec2.SecurityGroup("web-secgrp", {
             * ingress: [
             * { protocol: "tcp", fromPort: 80, toPort: 80, cidrBlocks: ["0.0.0.0/0"] },
             * ],
             * });
            */
            final var group = new io.pulumi.aws.ec2.SecurityGroup("web-secgrp", $ -> {
                //var ingress = new io.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs(List.of("0.0.0.0/0"), "", 80, new List(), "", "tcp", new List(), )
                var ingress = new io.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs.Builder(); 
                ingress.setProtocol("tcp")
                .setFromPort(80)
                .setToPort(80)
                .setCidrBlocks(List.of("0.0.0.0/0"));
                $.setIngress(List.of(
                    ingress.build()
                ));
            });
            /*
             * 
             * // (optional) create a simple web server using the startup script for the instance
             * const userData =
             * `#!/bin/bash
             * echo "Hello, World!" > index.html
             * nohup python -m SimpleHTTPServer 80 &`;
             */
            final var userData = 
             "#!/bin/bash"+
             "echo \"Hello, World!\" > index.html"+
             "nohup python -m SimpleHTTPServer 80 &";
            /*
             * 
             * const server = new aws.ec2.Instance("web-server-www", {
             * tags: { "Name": "web-server-www" },
             * instanceType: aws.ec2.InstanceType.T2_Micro, // t2.micro is available in the
             * AWS free tier
             * vpcSecurityGroupIds: [ group.id ], // reference the group object above
             * ami: ami,
             * userData: userData, // start a simple web server
             * });
             */
            final var server = new Instance("web-server-www", $ -> {
                $.setTags(Map.of(
                    "Name", "web-server-www"
                ))
                .setInstanceType(Input.ofRight(io.pulumi.aws.ec2.enums.InstanceType.T2_Micro))
                .setVpcSecurityGroupIds(List.of(group.getId().toString()))
                .setAmi(Input.of(ami))
                .setUserData(userData)
                ;
            });
            /*
             * 
             * export const publicIp = server.publicIp;
             * export const publicHostName = server.publicDns;
             */
            this.publicIp = server.getPublicIp();
            this.publicHostName = server.getPublicDns();
        }
    }
