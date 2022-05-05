package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;
import static com.pulumi.codegen.internal.Serialization.*;
import com.pulumi.codegen.internal.KeyedValue;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
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

        final var zones = Output.of(AwsFunctions.getAvailabilityZones());

        final var vpcSubnet = zones.apply(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<Subnet>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.names()) {
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

        final var rta = zones.apply(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<RouteTableAssociation>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.names()) {
                var resource = new RouteTableAssociation("rta-" + range.key(), RouteTableAssociationArgs.builder()                
                    .routeTableId(eksRouteTable.id())
                    .subnetId(vpcSubnet[range.key()].id())
                    .build());

                resources.add(resource);
            }

            return resources;
        });

        final var subnetIds = vpcSubnet.stream().map(element -> element.id()).collect(toList());

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
        ctx.export("kubeconfig", Output.tuple(eksCluster.endpoint(), eksCluster.certificateAuthority(), eksCluster.name()).apply(values -> {
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
