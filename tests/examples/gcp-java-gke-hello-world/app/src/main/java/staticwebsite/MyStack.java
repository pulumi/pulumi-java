package staticwebsite;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import io.pulumi.Config;
import io.pulumi.Stack;
import io.pulumi.core.Input;
import io.pulumi.core.Output;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.gcp.container.ClusterArgs;
import io.pulumi.gcp.container.GetEngineVersions;
import io.pulumi.gcp.container.NodePool;
import io.pulumi.gcp.container.NodePoolArgs;
import io.pulumi.gcp.container.inputs.GetEngineVersionsArgs;
import io.pulumi.gcp.container.inputs.NodePoolManagementArgs;
import io.pulumi.gcp.container.inputs.NodePoolNodeConfigArgs;
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
import io.pulumi.kubernetes.core_v1.inputs.ContainerArgs;
import io.pulumi.kubernetes.core_v1.inputs.ContainerPortArgs;
import io.pulumi.kubernetes.core_v1.inputs.PodSpecArgs;
import io.pulumi.kubernetes.core_v1.inputs.PodTemplateSpecArgs;
import io.pulumi.kubernetes.core_v1.inputs.ServicePortArgs;
import io.pulumi.kubernetes.core_v1.inputs.ServiceSpecArgs;
import io.pulumi.kubernetes.meta_v1.inputs.LabelSelectorArgs;
import io.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;
import io.pulumi.gcp.container.Cluster;
import io.pulumi.resources.CustomResourceOptions;

public final class MyStack extends Stack {

    @OutputExport(name="serviceName", type=String.class, parameters={})
    private Output<String> serviceName;

    @OutputExport(name="servicePublicIP", type=String.class, parameters={})
    private Output<String> servicePublicIP;

    @OutputExport(name="deploymentName", type=String.class, parameters={})
    private Output<String> deploymentName;

    @OutputExport(name="namespaceName", type=String.class, parameters={})
    private Output<String> namespaceName;

    @OutputExport(name="kubeconfig", type=String.class, parameters={})
    private Output<String> kubeconfig;

    @OutputExport(name="clusterName", type=String.class, parameters={})
    private Output<String> clusterName;

    @OutputExport(name="masterVersion", type=String.class, parameters={})
    private Output<String> masterVersion;

    public MyStack() throws Exception {
        /*
        const name = "helloworld";
        */
        final String name = "helloworld";
        /*
        const config = new pulumi.Config();
        export const masterVersion = config.get("masterVersion") ||
            gcp.container.getEngineVersions().then(it => it.latestMasterVersion);
        */
        final var config = io.pulumi.Config.of(); // TODO Config docs
        final var masterVersion = config.get("masterVersion").orElse(
            GetEngineVersions.invokeAsync(
                GetEngineVersionsArgs.Empty,
                InvokeOptions.Empty
            ).thenApply(thing -> thing.getLatestMasterVersion()).get());
        
        this.masterVersion = Output.of(masterVersion);
        /*
        /*
        // Create a GKE cluster
        const cluster = new gcp.container.Cluster(name, {
            // We can't create a cluster with no node pool defined, but we want to only use
            // separately managed node pools. So we create the smallest possible default
            // node pool and immediately delete it.
            initialNodeCount: 1,
            removeDefaultNodePool: true,

            minMasterVersion: masterVersion,
        });
        */
        final var cluster = new Cluster(name,
            ClusterArgs.builder()
            .setInitialNodeCount(1)
            .setRemoveDefaultNodePool(true)
            .setMinMasterVersion(masterVersion)
            .build(),
            CustomResourceOptions.Empty
        );
        /*
        
        const nodePool = new gcp.container.NodePool(`primary-node-pool`, {
            cluster: cluster.name,
            initialNodeCount: 2,
            location: cluster.location,
            nodeConfig: {
                preemptible: true,
                machineType: "n1-standard-1",
                oauthScopes: [
                    "https://www.googleapis.com/auth/compute",
                    "https://www.googleapis.com/auth/devstorage.read_only",
                    "https://www.googleapis.com/auth/logging.write",
                    "https://www.googleapis.com/auth/monitoring",
                ],
            },
            version: masterVersion,
            management: {
                autoRepair: true,
            },
        }, {
            dependsOn: [cluster],
        });
        */
        final var nodePool = new NodePool("primary-node-pool", // TODO add builder docstrings
            NodePoolArgs.builder()
            .setCluster(cluster.getName().toInput())
            .setLocation(cluster.getLocation().toInput())
            .setVersion(masterVersion)
            .setInitialNodeCount(2)
            .setNodeConfig(NodePoolNodeConfigArgs.builder()
                .setPreemptible(true)
                .setMachineType("n1-standard-1")
                .setOauthScopes(List.of(
                    "https://www.googleapis.com/auth/compute",
                    "https://www.googleapis.com/auth/devstorage.read_only",
                    "https://www.googleapis.com/auth/logging.write",
                    "https://www.googleapis.com/auth/monitoring"
                ))
                .build()
            )
            .setManagement(NodePoolManagementArgs.builder()
                .setAutoRepair(true)
                .build()
            )
            .build(),
            CustomResourceOptions.builder()
            .setDependsOn(List.of(cluster))
            .build());
        /*

        // Export the Cluster name
        export const clusterName = cluster.name;
        */
        this.clusterName = cluster.getName();
        System.out.println(nodePool.getResourceName());
        System.out.println(cluster.getResourceName());
        /*

        // Manufacture a GKE-style kubeconfig. Note that this is slightly "different"
        // because of the way GKE requires gcloud to be in the picture for cluster
        // authentication (rather than using the client cert/key directly).
        export const kubeconfig = pulumi.
            all([ cluster.name, cluster.endpoint, cluster.masterAuth ]).
            apply(([ name, endpoint, masterAuth ]) => {
                const context = `${gcp.config.project}_${gcp.config.zone}_${name}`;
                return `apiVersion: v1
        clusters:
        - cluster:
            certificate-authority-data: ${masterAuth.clusterCaCertificate}
            server: https://${endpoint}
        name: ${context}
        contexts:
        - context:
            cluster: ${context}
            user: ${context}
        name: ${context}
        current-context: ${context}
        kind: Config
        preferences: {}
        users:
        - name: ${context}
        user:
            auth-provider:
            config:
                cmd-args: config config-helper --format=json
                cmd-path: gcloud
                expiry-key: '{.credential.token_expiry}'
                token-key: '{.credential.access_token}'
            name: gcp
        `;
            });
        */
        //cluster.getName(), cluster.getEndpoint(), cluster.getMasterAuth()
        //const context = `${gcp.config.project}_${gcp.config.zone}_${name}`;
        final var gcpConfig = new io.pulumi.gcp.Config();
        var clusterName = gcpConfig.project().get() + "_" + gcpConfig.zone().get() + "_" + name;
        System.out.println(clusterName);

        var masterAuthClusterCaCertificate = cluster.getMasterAuth().applyOptional(args -> args.getClusterCaCertificate());
        this.kubeconfig = cluster.getEndpoint()
            .apply(endpoint -> masterAuthClusterCaCertificate.applyValue(
                caCert -> {
                    var retval = MessageFormat.format(String.join("\n",
                        "apiVersion: v1",
                        "clusters:",
                        "- cluster:",
                        "    certificate-authority-data: {2}",
                        "    server: https://{1}",
                        "name: {0}",
                        "contexts:",
                        "- context:",
                        "    cluster: {0}",
                        "    user: {0}",
                        "name: {0}",
                        "current-context: {0}",
                        "kind: Config",
                        "preferences: '{}'",
                        "users:",
                        "- name: {0}",
                        "user:",
                        "    auth-provider:",
                        "    config:",
                        "        cmd-args: config config-helper --format=json",
                        "        cmd-path: gcloud",
                        "        expiry-key: '{.credential.token_expiry}'",
                        "        token-key: '{.credential.access_token}'",
                        "    name: gcp"
                    ), clusterName, endpoint, caCert);
                    System.out.println(retval);
                    return retval;
                }
            ));
        /*"
        // Create a Kubernetes provider instance that uses our cluster from above.
        const clusterProvider = new k8s.Provider(name, {
            kubeconfig: kubeconfig,
        }, {
        dependsOn: [nodePool],
        });
        */
        final var clusterProvider = new Provider(name,
            ProviderArgs.builder()
                .setKubeconfig(this.kubeconfig.toInput())
                .build(),
            CustomResourceOptions.builder()
                .setDependsOn(List.of(nodePool, cluster))
                .build());
        final var clusterResourceOptions = CustomResourceOptions.builder()
            .setProvider(clusterProvider)
            .build();
        /*

        // Create a Kubernetes Namespace
        const ns = new k8s.core.v1.Namespace(name, {}, { provider: clusterProvider });

        // Export the Namespace name
        export const namespaceName = ns.metadata.name;
        */
        final var ns = new Namespace(name,
            NamespaceArgs.Empty,
            clusterResourceOptions
        );

        this.namespaceName = ns.getMetadata().applyOptional(arg0 -> arg0.getName());
        /*
        // Create a NGINX Deployment
        const appLabels = { appClass: name };
        const deployment = new k8s.apps.v1.Deployment(name,
            {
                metadata: {
                    namespace: namespaceName,
                    labels: appLabels,
                },
                spec: {
                    replicas: 1,
                    selector: { matchLabels: appLabels },
                    template: {
                        metadata: {
                            labels: appLabels, // TODO verify modified and reused metadata
                        },
                        spec: {
                            containers: [
                                {
                                    name: name,
                                    image: "nginx:latest",
                                    ports: [{ name: "http", containerPort: 80 }],
                                },
                            ],
                        },
                    },
                },
            },
            {
                provider: clusterProvider,
            },
        );
        */
        final var appLabels = Map.of("appClass", name);
        
        final var metadata = ObjectMetaArgs.builder()
                .setNamespace(namespaceName.toInput())
                .setLabels(appLabels)
                .build();

        final var deployment = new Deployment(name, DeploymentArgs.builder()
            .setMetadata(metadata)
            .setSpec(DeploymentSpecArgs.builder()
                .setReplicas(1)
                .setSelector(LabelSelectorArgs.builder()
                    .setMatchLabels(appLabels)
                    .build())
                .setTemplate(PodTemplateSpecArgs.builder()
                    .setMetadata(metadata)
                    .setSpec(PodSpecArgs.builder()
                        .setContainers(List.of(ContainerArgs.builder()
                            .setName(name)
                            .setImage("nginx:latest")
                            .setPorts(List.of(ContainerPortArgs.builder()
                                .setName("http")
                                .setContainerPort(80)
                                .build()))
                            .build()))
                        .build())
                    .build())
                .build())
            .build(), clusterResourceOptions);
        /*

        // Export the Deployment name
        export const deploymentName = deployment.metadata.name;
        */
        this.deploymentName = deployment.getMetadata().applyOptional(arg0 -> arg0.getName());
        /*

        // Create a LoadBalancer Service for the NGINX Deployment
        const service = new k8s.core.v1.Service(name,
            {
                metadata: {
                    labels: appLabels,
                    namespace: namespaceName,
                },
                spec: {
                    type: "LoadBalancer",
                    ports: [{ port: 80, targetPort: "http" }],
                    selector: appLabels,
                },
            },
            {
                provider: clusterProvider,
            },
        );
        */
        final var service = new Service(name, ServiceArgs.builder()
            .setMetadata(metadata)
            .setSpec(ServiceSpecArgs.builder()
                .setType(Input.ofRight(ServiceSpecType.LoadBalancer))
                .setPorts(List.of(ServicePortArgs.builder()
                    .setPort(80)
                    .setTargetPort(Input.ofRight("http"))
                    .build()))
                .setSelector(appLabels)
                .build())
            .build(), clusterResourceOptions);
        /*
        // Export the Service name and public LoadBalancer endpoint
        export const serviceName = service.metadata.name;
        export const servicePublicIP = service.status.loadBalancer.ingress[0].ip;
        */
        this.serviceName = service.getMetadata().applyOptional(arg0 -> arg0.getName());
        this.servicePublicIP = service.getStatus()
            .applyOptional(arg0 -> arg0.getLoadBalancer())
            .applyOptional(arg0 -> arg0.getIngress().get(0).getIp());
    }
}
