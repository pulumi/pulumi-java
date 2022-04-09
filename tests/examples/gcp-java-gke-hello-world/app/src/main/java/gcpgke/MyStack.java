package gcpgke;

import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.Export;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.gcp.container.*;
import io.pulumi.gcp.container.inputs.GetEngineVersionsArgs;
import io.pulumi.gcp.container.inputs.NodePoolManagementArgs;
import io.pulumi.gcp.container.inputs.NodePoolNodeConfigArgs;
import io.pulumi.gcp.container.outputs.GetEngineVersionsResult;
import io.pulumi.kubernetes.Provider;
import io.pulumi.kubernetes.ProviderArgs;
import io.pulumi.kubernetes.apps_v1.Deployment;
import io.pulumi.kubernetes.apps_v1.DeploymentArgs;
import io.pulumi.kubernetes.apps_v1.inputs.DeploymentSpecArgs;
import io.pulumi.kubernetes.core_v1.Namespace;
import io.pulumi.kubernetes.core_v1.NamespaceArgs;
import io.pulumi.kubernetes.core_v1.Service;
import io.pulumi.kubernetes.core_v1.ServiceArgs;
import io.pulumi.kubernetes.core_v1.enums.ServiceSpecType;
import io.pulumi.kubernetes.core_v1.inputs.*;
import io.pulumi.kubernetes.meta_v1.inputs.LabelSelectorArgs;
import io.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;
import io.pulumi.resources.CustomResourceOptions;

import java.text.MessageFormat;
import java.util.Map;

public final class MyStack extends Stack {

    @Export(name="serviceName", type=String.class)
    private final Output<String> serviceName;

    @Export(name="servicePublicIP", type=String.class)
    private final Output<String> servicePublicIP;

    @Export(name="deploymentName", type=String.class)
    private final Output<String> deploymentName;

    @Export(name="namespaceName", type=String.class)
    private final Output<String> namespaceName;

    @Export(name="kubeconfig", type=String.class)
    private final Output<String> kubeconfig;

    @Export(name="clusterName", type=String.class)
    private final Output<String> clusterName;

    @Export(name="masterVersion", type=String.class)
    private final Output<String> masterVersion;

    public MyStack() {
        final String name = "helloworld";

        final var config = io.pulumi.Config.of();
        final var masterVersion = config.get("masterVersion").orElse(
            GetEngineVersions.invokeAsync(
                GetEngineVersionsArgs.Empty,
                InvokeOptions.Empty
            ).thenApply(GetEngineVersionsResult::getLatestMasterVersion).join()
        );

        this.masterVersion = Output.of(masterVersion);

        // Create a GKE cluster
        // We can't create a cluster with no node pool defined, but we want to only use
        // separately managed node pools. So we create the smallest possible default
        // node pool and immediately delete it.
        final var cluster = new Cluster(name,
            ClusterArgs.builder().initialNodeCount(1)
            .removeDefaultNodePool(true)
            .minMasterVersion(masterVersion)
            .build()
        );

        final var nodePool = new NodePool("primary-node-pool",
            NodePoolArgs.builder()
            .cluster(cluster.getName())
            .location(cluster.getLocation())
            .version(masterVersion)
            .initialNodeCount(2)
            .nodeConfig(NodePoolNodeConfigArgs.builder()
                .preemptible(true)
                .machineType("n1-standard-1")
                .oauthScopes(
                    "https://www.googleapis.com/auth/compute",
                    "https://www.googleapis.com/auth/devstorage.read_only",
                    "https://www.googleapis.com/auth/logging.write",
                    "https://www.googleapis.com/auth/monitoring"
                )
                .build()
            )
            .management(NodePoolManagementArgs.builder()
                .autoRepair(true)
                .build()
            )
            .build(),
            CustomResourceOptions.builder()
            .dependsOn(cluster)
            .build());
        this.clusterName = cluster.getName();

        // Manufacture a GKE-style kubeconfig. Note that this is slightly "different"
        // because of the way GKE requires gcloud to be in the picture for cluster
        // authentication (rather than using the client cert/key directly).
        final var gcpConfig = new io.pulumi.gcp.Config();
        var clusterName = String.format("%s_%s_%s",
                gcpConfig.project().orElseThrow(),
                gcpConfig.zone().orElseThrow(),
                name
        );

        var masterAuthClusterCaCertificate = cluster.getMasterAuth()
                .applyValue(a -> a.getClusterCaCertificate().orElseThrow());

        this.kubeconfig = cluster.getEndpoint()
            .apply(endpoint -> masterAuthClusterCaCertificate.applyValue(
                caCert -> MessageFormat.format(String.join("\n",
                    "apiVersion: v1",
                    "clusters:",
                    "- cluster:",
                    "    certificate-authority-data: {2}",
                    "    server: https://{1}",
                    "  name: {0}",
                    "contexts:",
                    "- context:",
                    "    cluster: {0}",
                    "    user: {0}",
                    "  name: {0}",
                    "current-context: {0}",
                    "kind: Config",
                    "preferences: '{}'",
                    "users:",
                    "- name: {0}",
                    "  user:",
                    "    auth-provider:",
                    "      config:",
                    "        cmd-args: config config-helper --format=json",
                    "        cmd-path: gcloud",
                    "        expiry-key: \"'{.credential.token_expiry}'\"",
                    "        token-key: \"'{.credential.access_token}'\"",
                    "      name: gcp"
                ), clusterName, endpoint, caCert)
            ));

        // Create a Kubernetes provider instance that uses our cluster from above.
        final var clusterProvider = new Provider(name,
            ProviderArgs.builder()
                .kubeconfig(this.kubeconfig)
                .build(),
            CustomResourceOptions.builder()
                .dependsOn(nodePool, cluster)
                .build()
        );
        final var clusterResourceOptions = CustomResourceOptions.builder()
            .provider(clusterProvider)
            .build();

        // Create a Kubernetes Namespace
        final var ns = new Namespace(name,
            NamespaceArgs.Empty,
            clusterResourceOptions
        );

        // Export the Namespace name
        this.namespaceName = ns.getMetadata().apply(m -> Output.of(m.getName().orElseThrow()));

        final var appLabels = Map.of("appClass", name);

        final var metadata = ObjectMetaArgs.builder()
                .namespace(namespaceName)
                .labels(appLabels)
                .build();

        // Create a NGINX Deployment
        final var deployment = new Deployment(name, DeploymentArgs.builder()
            .metadata(metadata)
            .spec(DeploymentSpecArgs.builder()
                .replicas(1)
                .selector(LabelSelectorArgs.builder()
                    .matchLabels(appLabels)
                    .build())
                .template(PodTemplateSpecArgs.builder()
                    .metadata(metadata)
                    .spec(PodSpecArgs.builder()
                        .containers(ContainerArgs.builder()
                            .name(name)
                            .image("nginx:latest")
                            .ports(ContainerPortArgs.builder()
                                .name("http")
                                .containerPort(80)
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build(), clusterResourceOptions);

        // Export the Deployment name
        this.deploymentName = deployment.getMetadata().apply(m -> Output.of(m.getName().orElseThrow()));

        // Create a LoadBalancer Service for the NGINX Deployment
        final var service = new Service(name, ServiceArgs.builder()
            .metadata(metadata)
            .spec(ServiceSpecArgs.builder()
                .type(Output.ofRight(ServiceSpecType.LoadBalancer))
                .ports(ServicePortArgs.builder()
                    .port(80)
                    .targetPort(Output.ofRight("http"))
                    .build())
                .selector(appLabels)
                .build())
            .build(), clusterResourceOptions);

        // Export the Service name and public LoadBalancer endpoint
        this.serviceName = service.getMetadata().applyValue(m -> m.getName().orElseThrow());
        this.servicePublicIP = service.getStatus()
            .applyValue(s -> s.getLoadBalancer().orElseThrow())
            .applyValue(status -> status.getIngress().get(0).getIp().orElseThrow());
    }
}
