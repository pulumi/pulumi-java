package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.VpcEndpoint;
import com.pulumi.aws.ec2.VpcEndpointArgs;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.inputs.ec2.GetPrefixListArgs;
import com.pulumi.aws.ec2.NetworkAcl;
import com.pulumi.aws.ec2.NetworkAclArgs;
import com.pulumi.aws.ec2.NetworkAclRule;
import com.pulumi.aws.ec2.NetworkAclRuleArgs;
import com.pulumi.aws.ec2.inputs.GetAmiIdsArgs;
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
        var aws_vpc = new Vpc("aws_vpc", VpcArgs.builder()        
            .cidrBlock("10.0.0.0/16")
            .instanceTenancy("default")
            .build());

        var privateS3VpcEndpoint = new VpcEndpoint("privateS3VpcEndpoint", VpcEndpointArgs.builder()        
            .vpcId(aws_vpc.id())
            .serviceName("com.amazonaws.us-west-2.s3")
            .build());

        final var privateS3PrefixList = Ec2Functions.getPrefixList(GetPrefixListArgs.builder()
            .prefixListId(privateS3VpcEndpoint.prefixListId())
            .build());

        var bar = new NetworkAcl("bar", NetworkAclArgs.builder()        
            .vpcId(aws_vpc.id())
            .build());

        var privateS3NetworkAclRule = new NetworkAclRule("privateS3NetworkAclRule", NetworkAclRuleArgs.builder()        
            .networkAclId(bar.id())
            .ruleNumber(200)
            .egress(false)
            .protocol("tcp")
            .ruleAction("allow")
            .cidrBlock(privateS3PrefixList.applyValue(getPrefixListResult -> getPrefixListResult).applyValue(privateS3PrefixList -> privateS3PrefixList.applyValue(getPrefixListResult -> getPrefixListResult.cidrBlocks()[0])))
            .fromPort(443)
            .toPort(443)
            .build());

        final var amis = Ec2Functions.getAmiIds(GetAmiIdsArgs.builder()
            .owners(bar.id())
            .filters(GetAmiIdsFilterArgs.builder()
                .name(bar.id())
                .values("pulumi*")
                .build())
            .build());

    }
}
