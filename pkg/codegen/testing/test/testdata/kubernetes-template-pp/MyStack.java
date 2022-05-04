package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var argocd_serverDeployment = new Deployment("argocd_serverDeployment", DeploymentArgs.builder()        
            .apiVersion("apps/v1")
            .kind("Deployment")
            .metadata(ObjectMetaArgs.builder()
                .name("argocd-server")
                .build())
            .spec(DeploymentSpecArgs.builder()
                .template(PodTemplateSpecArgs.builder()
                    .spec(PodSpecArgs.builder()
                        .containers(ContainerArgs.builder()
                            .readinessProbe(ProbeArgs.builder()
                                .httpGet(HTTPGetActionArgs.builder()
                                    .port(8080)
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build());

    }
}
