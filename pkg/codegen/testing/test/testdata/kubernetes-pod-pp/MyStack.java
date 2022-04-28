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
        var bar = new Pod("bar", PodArgs.builder()        
            .apiVersion("v1")
            .kind("Pod")
            .metadata(ObjectMeta.builder()
                .namespace("foo")
                .name("bar")
                .build())
            .spec(PodSpec.builder()
                .containers(Container.builder()
                    .name("nginx")
                    .image("nginx:1.14-alpine")
                    .resources(ResourceRequirements.builder()
                        .limits(Map.ofEntries(
                            Map.entry("memory", "20Mi"),
                            Map.entry("cpu", 0.2)
                        ))
                        .build())
                    .build())
                .build())
            .build());

        return ctx.exports();
    }
}
