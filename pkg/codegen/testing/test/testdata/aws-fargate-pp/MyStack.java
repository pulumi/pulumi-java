package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;
import static com.pulumi.codegen.internal.Serialization.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        final var vpc = Output.of(Ec2Functions.getVpc(GetVpcArgs.builder()
            .default_(true)
            .build()));

        final var subnets = Output.of(Ec2Functions.getSubnetIds(GetSubnetIdsArgs.builder()
            .vpcId(vpc.apply(getVpcResult -> getVpcResult.id()))
            .build()));

        var webSecurityGroup = new SecurityGroup("webSecurityGroup", SecurityGroupArgs.builder()        
            .vpcId(vpc.apply(getVpcResult -> getVpcResult.id()))
            .egress(SecurityGroupEgressArgs.builder()
                .protocol("-1")
                .fromPort(0)
                .toPort(0)
                .cidrBlocks("0.0.0.0/0")
                .build())
            .ingress(SecurityGroupIngressArgs.builder()
                .protocol("tcp")
                .fromPort(80)
                .toPort(80)
                .cidrBlocks("0.0.0.0/0")
                .build())
            .build());

        var cluster = new Cluster("cluster");

        var taskExecRole = new Role("taskExecRole", RoleArgs.builder()        
            .assumeRolePolicy(serializeJson(
                jsonObject(
                    jsonProperty("Version", "2008-10-17"),
                    jsonProperty("Statement", jsonArray(jsonObject(
                        jsonProperty("Sid", ""),
                        jsonProperty("Effect", "Allow"),
                        jsonProperty("Principal", jsonObject(
                            jsonProperty("Service", "ecs-tasks.amazonaws.com")
                        )),
                        jsonProperty("Action", "sts:AssumeRole")
                    )))
                )))
            .build());

        var taskExecRolePolicyAttachment = new RolePolicyAttachment("taskExecRolePolicyAttachment", RolePolicyAttachmentArgs.builder()        
            .role(taskExecRole.name())
            .policyArn("arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy")
            .build());

        var webLoadBalancer = new LoadBalancer("webLoadBalancer", LoadBalancerArgs.builder()        
            .subnets(subnets.apply(getSubnetIdsResult -> getSubnetIdsResult.ids()))
            .securityGroups(webSecurityGroup.id())
            .build());

        var webTargetGroup = new TargetGroup("webTargetGroup", TargetGroupArgs.builder()        
            .port(80)
            .protocol("HTTP")
            .targetType("ip")
            .vpcId(vpc.apply(getVpcResult -> getVpcResult.id()))
            .build());

        var webListener = new Listener("webListener", ListenerArgs.builder()        
            .loadBalancerArn(webLoadBalancer.arn())
            .port(80)
            .defaultActions(ListenerDefaultActionArgs.builder()
                .type("forward")
                .targetGroupArn(webTargetGroup.arn())
                .build())
            .build());

        var appTask = new TaskDefinition("appTask", TaskDefinitionArgs.builder()        
            .family("fargate-task-definition")
            .cpu("256")
            .memory("512")
            .networkMode("awsvpc")
            .requiresCompatibilities("FARGATE")
            .executionRoleArn(taskExecRole.arn())
            .containerDefinitions(serializeJson(
                jsonArray(jsonObject(
                    jsonProperty("name", "my-app"),
                    jsonProperty("image", "nginx"),
                    jsonProperty("portMappings", jsonArray(jsonObject(
                        jsonProperty("containerPort", 80),
                        jsonProperty("hostPort", 80),
                        jsonProperty("protocol", "tcp")
                    )))
                ))))
            .build());

        var appService = new Service("appService", ServiceArgs.builder()        
            .cluster(cluster.arn())
            .desiredCount(5)
            .launchType("FARGATE")
            .taskDefinition(appTask.arn())
            .networkConfiguration(ServiceNetworkConfigurationArgs.builder()
                .assignPublicIp(true)
                .subnets(subnets.apply(getSubnetIdsResult -> getSubnetIdsResult.ids()))
                .securityGroups(webSecurityGroup.id())
                .build())
            .loadBalancers(ServiceLoadBalancerArgs.builder()
                .targetGroupArn(webTargetGroup.arn())
                .containerName("my-app")
                .containerPort(80)
                .build())
            .build());

        ctx.export("url", webLoadBalancer.dnsName());
    }
}
