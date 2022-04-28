package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static Exports stack(Context ctx) {
        var securityGroup = new SecurityGroup("securityGroup", SecurityGroupArgs.builder()        
            .ingress(SecurityGroupIngress.builder()
                .protocol("tcp")
                .fromPort(0)
                .toPort(0)
                .cidrBlocks("0.0.0.0/0")
                .build())
            .build());

        final var ami = Output.of(AwsFunctions.getAmi(GetAmiArgs.builder()
            .filters(GetAmiFilter.builder()
                .name("name")
                .values("amzn-ami-hvm-*-x86_64-ebs")
                .build())
            .owners("137112412989")
            .mostRecent(true)
            .build()));

        var server = new Instance("server", InstanceArgs.builder()        
            .tags(Map.of("Name", "web-server-www"))
            .instanceType("t2.micro")
            .securityGroups(securityGroup.getName())
            .ami(ami.apply(getAmiResult -> getAmiResult.getId()))
            .userData("""
#!/bin/bash
echo "Hello, World!" > index.html
nohup python -m SimpleHTTPServer 80 &
            """)
            .build());

        ctx.export("publicIp", server.getPublicIp());
        ctx.export("publicHostName", server.getPublicDns());
        return ctx.exports();
    }
}
