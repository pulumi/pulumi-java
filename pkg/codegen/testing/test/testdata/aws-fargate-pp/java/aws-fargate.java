package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.ecs.Cluster;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicyAttachment;
import com.pulumi.aws.iam.RolePolicyAttachmentArgs;
import com.pulumi.aws.elasticloadbalancingv2.LoadBalancer;
import com.pulumi.aws.elasticloadbalancingv2.LoadBalancerArgs;
import com.pulumi.aws.elasticloadbalancingv2.TargetGroup;
import com.pulumi.aws.elasticloadbalancingv2.TargetGroupArgs;
import com.pulumi.aws.elasticloadbalancingv2.Listener;
import com.pulumi.aws.elasticloadbalancingv2.ListenerArgs;
import com.pulumi.aws.elasticloadbalancingv2.inputs.ListenerDefaultActionArgs;
import com.pulumi.aws.ecs.TaskDefinition;
import com.pulumi.aws.ecs.TaskDefinitionArgs;
import com.pulumi.aws.ecs.Service;
import com.pulumi.aws.ecs.ServiceArgs;
import com.pulumi.aws.ecs.inputs.ServiceNetworkConfigurationArgs;
import com.pulumi.aws.ecs.inputs.ServiceLoadBalancerArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import com.pulumi.resources.CustomResourceOptions;
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
        // Read the default VPC and public subnets, which we will use.
        final var vpc = Ec2Functions.getVpc(GetVpcArgs.builder()
            .default_(true)
            .build());

        final var subnets = Ec2Functions.getSubnetIds(GetSubnetIdsArgs.builder()
            .vpcId(vpc.applyValue(getVpcResult -> getVpcResult.id()))
            .build());

        // Create a security group that permits HTTP ingress and unrestricted egress.
        var webSecurityGroup = new SecurityGroup("webSecurityGroup", SecurityGroupArgs.builder()        
            .vpcId(vpc.applyValue(getVpcResult -> getVpcResult.id()))
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

        // Create an ECS cluster to run a container-based service.
        var cluster = new Cluster("cluster");

        // Create an IAM role that can be used by our service's task.
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

        // Create a load balancer to listen for HTTP traffic on port 80.
        var webLoadBalancer = new LoadBalancer("webLoadBalancer", LoadBalancerArgs.builder()        
            .subnets(subnets.applyValue(getSubnetIdsResult -> getSubnetIdsResult.ids()))
            .securityGroups(webSecurityGroup.id())
            .build());

        var webTargetGroup = new TargetGroup("webTargetGroup", TargetGroupArgs.builder()        
            .port(80)
            .protocol("HTTP")
            .targetType("ip")
            .vpcId(vpc.applyValue(getVpcResult -> getVpcResult.id()))
            .build());

        var webListener = new Listener("webListener", ListenerArgs.builder()        
            .loadBalancerArn(webLoadBalancer.arn())
            .port(80)
            .defaultActions(ListenerDefaultActionArgs.builder()
                .type("forward")
                .targetGroupArn(webTargetGroup.arn())
                .build())
            .build());

        // Spin up a load balanced service running NGINX
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
                .subnets(subnets.applyValue(getSubnetIdsResult -> getSubnetIdsResult.ids()))
                .securityGroups(webSecurityGroup.id())
                .build())
            .loadBalancers(ServiceLoadBalancerArgs.builder()
                .targetGroupArn(webTargetGroup.arn())
                .containerName("my-app")
                .containerPort(80)
                .build())
            .build(), CustomResourceOptions.builder()
                .dependsOn(webListener)
                .build());

        ctx.export("url", webLoadBalancer.dnsName());
    }
}
