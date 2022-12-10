package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.core_v1.Namespace;
import com.pulumi.kubernetes.apps_v1.Deployment;
import com.pulumi.kubernetes.apps_v1.DeploymentArgs;
import com.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;
import com.pulumi.kubernetes.apps_v1.inputs.DeploymentSpecArgs;
import com.pulumi.kubernetes.meta_v1.inputs.LabelSelectorArgs;
import com.pulumi.kubernetes.core_v1.inputs.PodTemplateSpecArgs;
import com.pulumi.kubernetes.core_v1.inputs.PodSpecArgs;
import com.pulumi.kubernetes.core_v1.Service;
import com.pulumi.kubernetes.core_v1.ServiceArgs;
import com.pulumi.kubernetes.core_v1.inputs.ServiceSpecArgs;
import com.pulumi.kubernetes.networking.k8s.io_v1.Ingress;
import com.pulumi.kubernetes.networking.k8s.io_v1.IngressArgs;
import com.pulumi.kubernetes.networking.k8s.io_v1.inputs.IngressSpecArgs;
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
        final var config = ctx.config();
        final var hostname = config.get("hostname").orElse("example.com");
        var nginxDemo = new Namespace("nginxDemo");

        var app = new Deployment("app", DeploymentArgs.builder()        
            .metadata(ObjectMetaArgs.builder()
                .namespace(nginxDemo.metadata().applyValue(metadata -> metadata.name()))
                .build())
            .spec(DeploymentSpecArgs.builder()
                .selector(LabelSelectorArgs.builder()
                    .matchLabels(Map.of("app.kubernetes.io/name", "nginx-demo"))
                    .build())
                .replicas(1)
                .template(PodTemplateSpecArgs.builder()
                    .metadata(ObjectMetaArgs.builder()
                        .labels(Map.of("app.kubernetes.io/name", "nginx-demo"))
                        .build())
                    .spec(PodSpecArgs.builder()
                        .containers(ContainerArgs.builder()
                            .name("app")
                            .image("nginx:1.15-alpine")
                            .build())
                        .build())
                    .build())
                .build())
            .build());

        var service = new Service("service", ServiceArgs.builder()        
            .metadata(ObjectMetaArgs.builder()
                .namespace(nginxDemo.metadata().applyValue(metadata -> metadata.name()))
                .labels(Map.of("app.kubernetes.io/name", "nginx-demo"))
                .build())
            .spec(ServiceSpecArgs.builder()
                .type("ClusterIP")
                .ports(ServicePortArgs.builder()
                    .port(80)
                    .targetPort(80)
                    .protocol("TCP")
                    .build())
                .selector(Map.of("app.kubernetes.io/name", "nginx-demo"))
                .build())
            .build());

        var ingress = new Ingress("ingress", IngressArgs.builder()        
            .metadata(ObjectMetaArgs.builder()
                .namespace(nginxDemo.metadata().applyValue(metadata -> metadata.name()))
                .build())
            .spec(IngressSpecArgs.builder()
                .rules(IngressRuleArgs.builder()
                    .host(hostname)
                    .http(HTTPIngressRuleValueArgs.builder()
                        .paths(HTTPIngressPathArgs.builder()
                            .path("/")
                            .pathType("Prefix")
                            .backend(IngressBackendArgs.builder()
                                .service(IngressServiceBackendArgs.builder()
                                    .name(service.metadata().applyValue(metadata -> metadata.name()))
                                    .port(ServiceBackendPortArgs.builder()
                                        .number(80)
                                        .build())
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build());

    }
}
