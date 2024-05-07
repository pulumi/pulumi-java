package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.AwsFunctions;
import com.pulumi.aws.inputs.GetAmiArgs;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        // Create a new security group for port 80.
        var securityGroup = new SecurityGroup("securityGroup", SecurityGroupArgs.builder()
            .ingress(SecurityGroupIngressArgs.builder()
                .protocol("tcp")
                .fromPort(0)
                .toPort(0)
                .cidrBlocks("0.0.0.0/0")
                .build())
            .build());

        // Get the ID for the latest Amazon Linux AMI.
        final var ami = AwsFunctions.getAmi(GetAmiArgs.builder()
            .filters(GetAmiFilterArgs.builder()
                .name("name")
                .values("amzn-ami-hvm-*-x86_64-ebs")
                .build())
            .owners("137112412989")
            .mostRecent(true)
            .build());

        // Create a simple web server using the startup script for the instance.
        var server = new Instance("server", InstanceArgs.builder()
            .tags(Map.of("Name", "web-server-www"))
            .instanceType("t2.micro")
            .securityGroups(securityGroup.name())
            .ami(ami.applyValue(getAmiResult -> getAmiResult.id()))
            .userData("""
#!/bin/bash
echo "Hello, World!" > index.html
nohup python -m SimpleHTTPServer 80 &
            """)
            .build());

        ctx.export("publicIp", server.publicIp());
        ctx.export("publicHostName", server.publicDns());
    }
}
