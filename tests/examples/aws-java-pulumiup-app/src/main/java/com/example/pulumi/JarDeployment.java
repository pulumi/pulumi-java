package com.example.pulumi;

import com.pulumi.asset.FileAsset;
import com.pulumi.aws.s3.BucketObject;
import com.pulumi.aws.s3.BucketObjectArgs;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.Provider;
import com.pulumi.kubernetes.ProviderArgs;
import com.pulumi.kubernetes.apps_v1.Deployment;
import com.pulumi.kubernetes.apps_v1.DeploymentArgs;
import com.pulumi.kubernetes.apps_v1.inputs.DeploymentSpecArgs;
import com.pulumi.kubernetes.core_v1.Namespace;
import com.pulumi.kubernetes.core_v1.NamespaceArgs;
import com.pulumi.kubernetes.core_v1.Service;
import com.pulumi.kubernetes.core_v1.ServiceArgs;
import com.pulumi.kubernetes.core_v1.enums.ServiceSpecType;
import com.pulumi.kubernetes.core_v1.inputs.ContainerArgs;
import com.pulumi.kubernetes.core_v1.inputs.ContainerPortArgs;
import com.pulumi.kubernetes.core_v1.inputs.EmptyDirVolumeSourceArgs;
import com.pulumi.kubernetes.core_v1.inputs.EnvVarArgs;
import com.pulumi.kubernetes.core_v1.inputs.PodSpecArgs;
import com.pulumi.kubernetes.core_v1.inputs.PodTemplateSpecArgs;
import com.pulumi.kubernetes.core_v1.inputs.ServicePortArgs;
import com.pulumi.kubernetes.core_v1.inputs.ServiceSpecArgs;
import com.pulumi.kubernetes.core_v1.inputs.VolumeArgs;
import com.pulumi.kubernetes.core_v1.inputs.VolumeMountArgs;
import com.pulumi.kubernetes.meta_v1.inputs.LabelSelectorArgs;
import com.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.CustomResourceOptions;

import java.util.List;
import java.util.Map;

public class JarDeployment extends ComponentResource {
    static final String TARGET_DIR = "/var/run/secrets/java";
    public final Output<String> endpoint;

    public JarDeployment(String name, LambdaArgs applyArgs) {
        super("example:spring:JarDeployment",
                name,
                JarDeployment.apply(applyArgs),
                null);

        // Check out JarDeploymentArgs
        final var args = JarDeployment.apply(applyArgs);

        final var appJar = deployJar(args.getJarPath(), args.getBucketName());
        this.endpoint = deployApp(args.getKubeconfig(), appJar);

        return;
    }

    public interface LambdaArgs {
         void op(JarDeploymentArgs.JarDeploymentArgsBuilder a);
    }
    private static JarDeploymentArgs apply(LambdaArgs applyArgs) {
        final var args = JarDeploymentArgs.builder();
        applyArgs.op(args);
        return args.build();
    }

    public BucketObject deployJar(String filepath, Output<String> jarBucket) {
        final var file = new FileAsset(filepath);

        final var appJar = new BucketObject("my-jar.jar",
                BucketObjectArgs.builder()
                        .bucket(jarBucket)
                        .source(file)
                        .build(),
                CustomResourceOptions.builder()
                        .parent(this)
                        .build());

        return appJar;
    }

    private static Output<String> getS3CpPath(BucketObject obj) {
        return Output.format("s3://%s/%s", obj.bucket(), obj.key());
    }

    public CustomResourceOptions clusterResourceOptions(Output<String> kubeconfig) {
        // Create a Kubernetes provider instance that uses our cluster from above.
        final var clusterProvider = new Provider("myProvider",
                ProviderArgs.builder()
                        .kubeconfig(kubeconfig)
                        .build(),
                CustomResourceOptions
                        .builder()
                        .parent(this)
                        .build());

        final var clusterResourceOptions = CustomResourceOptions.builder()
                .provider(clusterProvider)
                .parent(this)
                .build();
        return clusterResourceOptions;
    }

    public Output<String> deployApp(Output<String> kubeconfig, BucketObject appJar) {

        // Get resource options with kubernetes provider
        final var clusterResourceOptions = clusterResourceOptions(kubeconfig);

        // Create a Kubernetes Namespace
        final var namespace = new Namespace("ns-1", NamespaceArgs.builder()
                .build(),
                clusterResourceOptions);

        final var appLabels = Map.of(
        "appClass", "gs-spring-boot"
        );

        // Configure Metadata args
        final var metadata = ObjectMetaArgs.builder()
                .namespace(namespace.getId())
                .labels(appLabels)
                .build();

        /*
        // You can put your own asserts to make sure you don't run into weird misconfiguration issues twice
        if (configMap.getId().contains("/")) {
            throw new Exception("configMap name is invalid");
        }
        */

        // Create a Java deployment
        final var deployment = new Deployment("deployment", DeploymentArgs.builder()
                .metadata(metadata)
                .spec(DeploymentSpecArgs.builder()
                        .replicas(1)
                        .selector(LabelSelectorArgs.builder()
                                .matchLabels(appLabels)
                                .build())
                        .template(PodTemplateSpecArgs.builder()
                                .metadata(metadata)
                                .spec(PodSpecArgs.builder()
                                        .volumes(VolumeArgs.builder()
                                                .name("jar-volume")
                                                .emptyDir(EmptyDirVolumeSourceArgs.Empty)
                                                .build())
                                        .initContainers(ContainerArgs.builder()
                                                .name("init")
                                                .image("amazon/aws-cli")
                                                .command(
                                                        Output.tuple(getS3CpPath(appJar), Output.of(TARGET_DIR))
                                                                .applyValue(t -> List.of( "aws", "s3", "cp", t.t1, t.t2+"/app.jar")))
                                                .volumeMounts(
                                                        VolumeMountArgs.builder()
                                                                .name("jar-volume")
                                                                .mountPath(TARGET_DIR)
                                                                .build())
                                                .build())
                                        .containers(ContainerArgs.builder()
                                                .name("app")
                                                .image("openjdk")
                                                .command("java", "-jar", TARGET_DIR +"/app.jar")
                                                .env(EnvVarArgs.builder()
                                                        .name("APPLICATION_ETAG")
                                                        .value(appJar.etag())
                                                        .build())
                                                .volumeMounts(
                                                        VolumeMountArgs.builder()
                                                                .name("jar-volume")
                                                                .mountPath(TARGET_DIR)
                                                                .build())
                                                .ports(ContainerPortArgs.builder()
                                                        .name("http")
                                                        .containerPort(8080)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build(),
                clusterResourceOptions);
        // Create a LoadBalancer Service for the Deployment
        final var service = new Service("app-svc", ServiceArgs.builder()
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
        return service.status().applyValue(data -> data.get().loadBalancer().get().ingress().get(0).hostname().get()).applyValue(String::valueOf);
    }

}
