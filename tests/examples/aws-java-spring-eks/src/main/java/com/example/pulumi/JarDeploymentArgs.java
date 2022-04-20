package com.example.pulumi;

import com.pulumi.core.Output;
import com.pulumi.resources.ResourceArgs;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class JarDeploymentArgs extends ResourceArgs {
    private String jarPath;
    private Output<String> kubeconfig;
    private Output<String> bucketName;
    private Output<String> javaVersion = Output.of("8");
    private Output<Integer> replicas = Output.of(1);
    private Output<Integer> appPort = Output.of(8080);
}
