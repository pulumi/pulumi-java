package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static Exports stack(Context ctx) {
        var argocd_serverDeployment = new Deployment("argocd_serverDeployment", DeploymentArgs.builder()        
            .apiVersion("apps/v1")
            .kind("Deployment")
            .metadata(ObjectMeta.builder()
                .name("argocd-server")
                .build())
            .spec(DeploymentSpec.builder()
                .template(PodTemplateSpec.builder()
                    .spec(PodSpec.builder()
                        .containers(Container.builder()
                            .readinessProbe(Probe.builder()
                                .httpGet(HTTPGetAction.builder()
                                    .port(8080)
                                    .build())
                                .build())
                            .build())
                        .build())
                    .build())
                .build())
            .build());

        return ctx.exports();
    }
}
