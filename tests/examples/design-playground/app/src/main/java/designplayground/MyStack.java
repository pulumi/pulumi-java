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

import java.util.List;

public final class MyStack extends Stack {
    public MyStack() {
        final var service = new Service("myservice",
                ServiceArgs.builder()
                        // We are working to shorten .setMetadata() here to .metadata()
                        // https://github.com/pulumi/pulumi-java/issues/212
                        .setMetadata(ObjectMetaArgs.builder().setClusterName("clusterName").build())
                        .setSpec(ServiceSpecArgs.builder()
                                .setType(Either.ofRight(ServiceSpecType.LoadBalancer))
                                .setPorts(List.of(ServicePortArgs.builder()
                                        .setPort(80)
                                        // We should build overloads to make ofRight / Either wrapper disappear.
                                        .setTargetPort(Either.ofRight("http"))
                                        .build()))
                                .build())
                        .build()
        );
        Output<String> apiVersion = service.getApiVersion();
    }
}

// This version demonstrates currently supported lambda version to bind `ServiceArgs.builder`.
// This helps avoid importing the args class.
// This in turn avoids having to disambiguate similarly named args classes.
final class MyStackAlt extends Stack {
    public MyStackAlt() {
        final var service = new Service("myservice", builder ->
                builder
                        // Should we have lambda binder here also?
                        // .setMetadata(builder -> builder.setClusterName("clusterName").build())
                        // Name disambiguation and avoiding import is only really useful if this is done for nested
                        // types.
                        .setMetadata(ObjectMetaArgs.builder().setClusterName("clusterName").build())
                        .setSpec(ServiceSpecArgs.builder()
                                .setType(Either.ofRight(ServiceSpecType.LoadBalancer))
                                .setPorts(List.of(ServicePortArgs.builder()
                                        .setPort(80)
                                        .setTargetPort(Either.ofRight("http"))
                                        .build()))
                                .build())
                        .build()
        );
        Output<String> apiVersion = service.getApiVersion();
    }
}