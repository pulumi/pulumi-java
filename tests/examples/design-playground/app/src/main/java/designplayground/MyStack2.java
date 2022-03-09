package designplayground;

import designplayground.alt1.Service;
import designplayground.alt1.ServicePortArgs;
import designplayground.alt1.ServiceSpecType;
import io.pulumi.core.Output;

import java.util.List;

public class MyStack2 {

    public MyStack2() {
        final var service = new Service("myservice", args ->
                args.metadata($ -> $.clusterName("clusterName"))
                        .spec($ -> $.type(ServiceSpecType.LoadBalancer)
                                .ports(List.of(ServicePortArgs.builder()
                                        .port(80)
                                        .targetPort("http")
                                        .build())))
        );

        Output<String> apiVersion = service.getApiVersion();
    }
}


