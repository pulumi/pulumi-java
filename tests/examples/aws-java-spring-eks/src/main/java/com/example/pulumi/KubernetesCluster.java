package com.example.pulumi;

import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.Provider;
import com.pulumi.kubernetes.ProviderArgs;
import com.pulumi.resources.CustomResourceOptions;

import java.text.MessageFormat;

public class KubernetesCluster {
        /*
    public static CustomResourceOptions getResourceOptions(Context ctx) {
        final String name = "helloworld";

        final var masterVersion = ctx.config().get("masterVersion").orElse(
                ContainerFunctions.getEngineVersions()
                        .thenApply(GetEngineVersionsResult::latestMasterVersion).join()
        );

        ctx.export("masterVersion", Output.of(masterVersion));

        // Create a GKE cluster
        // We can't create a cluster with no node pool defined, but we want to only use
        // separately managed node pools. So we create the smallest possible default
        // node pool and immediately delete it.
        final var cluster = new Cluster(name,
                ClusterArgs.builder()
                        .initialNodeCount(1)
                        .minMasterVersion(masterVersion)
                        .build()
        );

        final var nodePool = new NodePool("primary-node-pool",
                NodePoolArgs.builder()
                        .cluster(cluster.name())
                        .location(cluster.location())
                        .version(masterVersion)
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
        ctx.export("clusterName", cluster.name());

        // Manufacture a GKE-style kubeconfig. Note that this is slightly "different"
        // because of the way GKE requires gcloud to be in the picture for cluster
        // authentication (rather than using the client cert/key directly).
        final var gcpConfig = new com.pulumi.gcp.Config();
        var clusterName = String.format("%s_%s_%s",
                gcpConfig.project().orElseThrow(),
                gcpConfig.zone().orElseThrow(),
                name
        );

        var masterAuthClusterCaCertificate = cluster.masterAuth()
                .applyValue(a -> a.clusterCaCertificate().orElseThrow());

        var kubeconfig = cluster.endpoint()
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
        kubeconfig.applyValue(val -> {System.out.println(val); return val;});
        ctx.export("kubeconfig", kubeconfig);
        // Create a Kubernetes provider instance that uses our cluster from above.
        final var clusterProvider = new Provider("myProvider",
                ProviderArgs.builder()
                        .kubeconfig(kubeconfig)
                        .build());
        final var clusterResourceOptions = CustomResourceOptions.builder()
                .provider(clusterProvider)
                .dependsOn(cluster)
                .build();
        return clusterResourceOptions;
    }
         */
}
