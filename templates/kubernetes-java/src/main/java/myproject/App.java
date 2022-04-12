package myproject;

import io.pulumi.Pulumi;
import io.pulumi.kubernetes.apps_v1.Deployment;
import io.pulumi.kubernetes.apps_v1.DeploymentArgs;
import io.pulumi.kubernetes.apps_v1.inputs.DeploymentSpecArgs;
import io.pulumi.kubernetes.core_v1.inputs.ContainerArgs;
import io.pulumi.kubernetes.core_v1.inputs.ContainerPortArgs;
import io.pulumi.kubernetes.core_v1.inputs.PodSpecArgs;
import io.pulumi.kubernetes.core_v1.inputs.PodTemplateSpecArgs;
import io.pulumi.kubernetes.meta_v1.inputs.LabelSelectorArgs;
import io.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(ctx -> {
            var labels = Map.of("app", "nginx");

            var deployment = new Deployment("nginx", DeploymentArgs.builder()
                    .spec(DeploymentSpecArgs.builder()
                            .selector(LabelSelectorArgs.builder()
                                    .matchLabels(labels)
                                    .build())
                            .replicas(1)
                            .template(PodTemplateSpecArgs.builder()
                                    .metadata(ObjectMetaArgs.builder()
                                            .labels(labels)
                                            .build())
                                    .spec(PodSpecArgs.builder()
                                            .containers(ContainerArgs.builder()
                                                    .name("nginx")
                                                    .image("nginx")
                                                    .ports(ContainerPortArgs.builder()
                                                            .containerPort(80)
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())

                            .build())
                    .build());

            var name = deployment.getMetadata().applyValue(m -> m.getName().orElse(""));

            ctx.export("name", name);
            return ctx.exports();
        });
        System.exit(exitCode);
    }
}
