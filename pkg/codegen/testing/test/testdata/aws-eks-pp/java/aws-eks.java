package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.InternetGateway;
import com.pulumi.aws.ec2.InternetGatewayArgs;
import com.pulumi.aws.ec2.RouteTable;
import com.pulumi.aws.ec2.RouteTableArgs;
import com.pulumi.aws.ec2.inputs.RouteTableRouteArgs;
import com.pulumi.aws.AwsFunctions;
import com.pulumi.aws.inputs.GetAvailabilityZonesArgs;
import com.pulumi.aws.ec2.Subnet;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.RouteTableAssociation;
import com.pulumi.aws.ec2.RouteTableAssociationArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicyAttachment;
import com.pulumi.aws.iam.RolePolicyAttachmentArgs;
import com.pulumi.aws.eks.Cluster;
import com.pulumi.aws.eks.ClusterArgs;
import com.pulumi.aws.eks.inputs.ClusterVpcConfigArgs;
import com.pulumi.aws.eks.NodeGroup;
import com.pulumi.aws.eks.NodeGroupArgs;
import com.pulumi.aws.eks.inputs.NodeGroupScalingConfigArgs;
import static com.pulumi.codegen.internal.Serialization.*;
import com.pulumi.codegen.internal.KeyedValue;
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
        // VPC
        var eksVpc = new Vpc("eksVpc", VpcArgs.builder()
            .cidrBlock("10.100.0.0/16")
            .instanceTenancy("default")
            .enableDnsHostnames(true)
            .enableDnsSupport(true)
            .tags(Map.of("Name", "pulumi-eks-vpc"))
            .build());

        var eksIgw = new InternetGateway("eksIgw", InternetGatewayArgs.builder()
            .vpcId(eksVpc.id())
            .tags(Map.of("Name", "pulumi-vpc-ig"))
            .build());

        var eksRouteTable = new RouteTable("eksRouteTable", RouteTableArgs.builder()
            .vpcId(eksVpc.id())
            .routes(RouteTableRouteArgs.builder()
                .cidrBlock("0.0.0.0/0")
                .gatewayId(eksIgw.id())
                .build())
            .tags(Map.of("Name", "pulumi-vpc-rt"))
            .build());

        // Subnets, one for each AZ in a region
        final var zones = AwsFunctions.getAvailabilityZones(GetAvailabilityZonesArgs.builder()
            .build());

        final var vpcSubnet = zones.applyValue(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<Subnet>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.names())) {
                var resource = new Subnet("vpcSubnet-" + range.key(), SubnetArgs.builder()
                    .assignIpv6AddressOnCreation(false)
                    .vpcId(eksVpc.id())
                    .mapPublicIpOnLaunch(true)
                    .cidrBlock(String.format("10.100.%s.0/24", range.key()))
                    .availabilityZone(range.value())
                    .tags(Map.of("Name", String.format("pulumi-sn-%s", range.value())))
                    .build());

                resources.add(resource);
            }

            return resources;
        });

        final var rta = zones.applyValue(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<RouteTableAssociation>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.names())) {
                var resource = new RouteTableAssociation("rta-" + range.key(), RouteTableAssociationArgs.builder()
                    .routeTableId(eksRouteTable.id())
                    .subnetId(vpcSubnet.applyValue(_vpcSubnet -> _vpcSubnet[range.key()].id()))
                    .build());

                resources.add(resource);
            }

            return resources;
        });

        final var subnetIds = vpcSubnet.applyValue(_vpcSubnet -> _vpcSubnet.stream().map(element -> element.id()).collect(toList()));

        var eksSecurityGroup = new SecurityGroup("eksSecurityGroup", SecurityGroupArgs.builder()
            .vpcId(eksVpc.id())
            .description("Allow all HTTP(s) traffic to EKS Cluster")
            .tags(Map.of("Name", "pulumi-cluster-sg"))
            .ingress(            
                SecurityGroupIngressArgs.builder()
                    .cidrBlocks("0.0.0.0/0")
                    .fromPort(443)
                    .toPort(443)
                    .protocol("tcp")
                    .description("Allow pods to communicate with the cluster API Server.")
                    .build(),
                SecurityGroupIngressArgs.builder()
                    .cidrBlocks("0.0.0.0/0")
                    .fromPort(80)
                    .toPort(80)
                    .protocol("tcp")
                    .description("Allow internet access to pods")
                    .build())
            .build());

        // EKS Cluster Role
        var eksRole = new Role("eksRole", RoleArgs.builder()
            .assumeRolePolicy(serializeJson(
                jsonObject(
                    jsonProperty("Version", "2012-10-17"),
                    jsonProperty("Statement", jsonArray(jsonObject(
                        jsonProperty("Action", "sts:AssumeRole"),
                        jsonProperty("Principal", jsonObject(
                            jsonProperty("Service", "eks.amazonaws.com")
                        )),
                        jsonProperty("Effect", "Allow"),
                        jsonProperty("Sid", "")
                    )))
                )))
            .build());

        var servicePolicyAttachment = new RolePolicyAttachment("servicePolicyAttachment", RolePolicyAttachmentArgs.builder()
            .role(eksRole.id())
            .policyArn("arn:aws:iam::aws:policy/AmazonEKSServicePolicy")
            .build());

        var clusterPolicyAttachment = new RolePolicyAttachment("clusterPolicyAttachment", RolePolicyAttachmentArgs.builder()
            .role(eksRole.id())
            .policyArn("arn:aws:iam::aws:policy/AmazonEKSClusterPolicy")
            .build());

        // EC2 NodeGroup Role
        var ec2Role = new Role("ec2Role", RoleArgs.builder()
            .assumeRolePolicy(serializeJson(
                jsonObject(
                    jsonProperty("Version", "2012-10-17"),
                    jsonProperty("Statement", jsonArray(jsonObject(
                        jsonProperty("Action", "sts:AssumeRole"),
                        jsonProperty("Principal", jsonObject(
                            jsonProperty("Service", "ec2.amazonaws.com")
                        )),
                        jsonProperty("Effect", "Allow"),
                        jsonProperty("Sid", "")
                    )))
                )))
            .build());

        var workerNodePolicyAttachment = new RolePolicyAttachment("workerNodePolicyAttachment", RolePolicyAttachmentArgs.builder()
            .role(ec2Role.id())
            .policyArn("arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy")
            .build());

        var cniPolicyAttachment = new RolePolicyAttachment("cniPolicyAttachment", RolePolicyAttachmentArgs.builder()
            .role(ec2Role.id())
            .policyArn("arn:aws:iam::aws:policy/AmazonEKSCNIPolicy")
            .build());

        var registryPolicyAttachment = new RolePolicyAttachment("registryPolicyAttachment", RolePolicyAttachmentArgs.builder()
            .role(ec2Role.id())
            .policyArn("arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly")
            .build());

        // EKS Cluster
        var eksCluster = new Cluster("eksCluster", ClusterArgs.builder()
            .roleArn(eksRole.arn())
            .tags(Map.of("Name", "pulumi-eks-cluster"))
            .vpcConfig(ClusterVpcConfigArgs.builder()
                .publicAccessCidrs("0.0.0.0/0")
                .securityGroupIds(eksSecurityGroup.id())
                .subnetIds(subnetIds)
                .build())
            .build());

        var nodeGroup = new NodeGroup("nodeGroup", NodeGroupArgs.builder()
            .clusterName(eksCluster.name())
            .nodeGroupName("pulumi-eks-nodegroup")
            .nodeRoleArn(ec2Role.arn())
            .subnetIds(subnetIds)
            .tags(Map.of("Name", "pulumi-cluster-nodeGroup"))
            .scalingConfig(NodeGroupScalingConfigArgs.builder()
                .desiredSize(2)
                .maxSize(2)
                .minSize(1)
                .build())
            .build());

        ctx.export("clusterName", eksCluster.name());
        ctx.export("kubeconfig", Output.tuple(eksCluster.endpoint(), eksCluster.certificateAuthority(), eksCluster.name()).applyValue(values -> {
            var endpoint = values.t1;
            var certificateAuthority = values.t2;
            var name = values.t3;
            return serializeJson(
                jsonObject(
                    jsonProperty("apiVersion", "v1"),
                    jsonProperty("clusters", jsonArray(jsonObject(
                        jsonProperty("cluster", jsonObject(
                            jsonProperty("server", endpoint),
                            jsonProperty("certificate-authority-data", certificateAuthority.data())
                        )),
                        jsonProperty("name", "kubernetes")
                    ))),
                    jsonProperty("contexts", jsonArray(jsonObject(
                        jsonProperty("contest", jsonObject(
                            jsonProperty("cluster", "kubernetes"),
                            jsonProperty("user", "aws")
                        ))
                    ))),
                    jsonProperty("current-context", "aws"),
                    jsonProperty("kind", "Config"),
                    jsonProperty("users", jsonArray(jsonObject(
                        jsonProperty("name", "aws"),
                        jsonProperty("user", jsonObject(
                            jsonProperty("exec", jsonObject(
                                jsonProperty("apiVersion", "client.authentication.k8s.io/v1alpha1"),
                                jsonProperty("command", "aws-iam-authenticator")
                            )),
                            jsonProperty("args", jsonArray(
                                "token", 
                                "-i", 
                                name
                            ))
                        ))
                    )))
                ));
        }));
    }
}
