package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.core_v1.Pod;
import com.pulumi.kubernetes.core_v1.PodArgs;
import com.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;
import com.pulumi.kubernetes.core_v1.inputs.PodSpecArgs;
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
        var bar = new Pod("bar", PodArgs.builder()
            .apiVersion("v1")
            .kind("Pod")
            .metadata(ObjectMetaArgs.builder()
                .namespace("foo")
                .name("bar")
                .build())
            .spec(PodSpecArgs.builder()
                .containers(ContainerArgs.builder()
                    .name("nginx")
                    .image("nginx:1.14-alpine")
                    .resources(ResourceRequirementsArgs.builder()
                        .limits(Map.ofEntries(
                            Map.entry("memory", "20Mi"),
                            Map.entry("cpu", "0.2")
                        ))
                        .build())
                    .build())
                .build())
            .build());

    }
}
