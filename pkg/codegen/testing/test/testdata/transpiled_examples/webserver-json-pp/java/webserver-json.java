package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
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
        final var config = ctx.config();
        final var instanceType = config.get("instanceType").orElse("t3.micro");
        var webSecGrp = new SecurityGroup("webSecGrp", SecurityGroupArgs.builder()        
            .ingress(SecurityGroupIngressArgs.builder()
                .protocol("tcp")
                .fromPort(80)
                .toPort(80)
                .cidrBlocks("0.0.0.0/0")
                .build())
            .build());

        var webServer = new Instance("webServer", InstanceArgs.builder()        
            .instanceType(instanceType)
            .ami(AwsFunctions.getAmi(GetAmiArgs.builder()
                .filters(GetAmiFilterArgs.builder()
                    .name("name")
                    .values("amzn-ami-hvm-*-x86_64-ebs")
                    .build())
                .owners("137112412989")
                .mostRecent(true)
                .build()).id())
            .userData(webSecGrp.arn().applyValue(arn -> String.join("""

            """,             
                "#!/bin/bash",
                String.format("echo 'Hello, World from %s!' > index.html", arn),
                "nohup python -m SimpleHTTPServer 80 &")))
            .vpcSecurityGroupIds(webSecGrp.id())
            .build());

        ctx.export("instanceId", webServer.id());
        ctx.export("publicIp", webServer.publicIp());
        ctx.export("publicHostName", webServer.publicDns());
    }
}
