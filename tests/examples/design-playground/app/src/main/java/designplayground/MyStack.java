package designplayground;

import io.pulumi.Stack;
import io.pulumi.core.Either;
import io.pulumi.core.Output;
import io.pulumi.kubernetes.core_v1.Service;
import io.pulumi.kubernetes.core_v1.ServiceArgs;
import io.pulumi.kubernetes.core_v1.enums.ServiceSpecType;
import io.pulumi.kubernetes.core_v1.inputs.ServicePortArgs;
import io.pulumi.kubernetes.core_v1.inputs.ServiceSpecArgs;
import io.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;

public final class MyStack extends Stack {
    public MyStack() {
        final var service = new Service("myservice",
                ServiceArgs.builder()
                        .metadata(ObjectMetaArgs.builder().clusterName("clusterName").build())
                        .spec(ServiceSpecArgs.builder()
                                // TODO[pulumi/pulumi-jvm#138] unroll Either
                                .type(Either.ofRight(ServiceSpecType.LoadBalancer))
                                .ports(ServicePortArgs.builder()
                                        .port(80)
                                        // TODO[pulumi/pulumi-jvm#138] unroll Either
                                        .targetPort(Either.ofRight("http"))
                                        .build())
                                .build())
                        .build()
        );
        Output<String> apiVersion = service.getApiVersion();
    }
}
