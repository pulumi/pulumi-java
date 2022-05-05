package com.example.pulumi;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;

public class PulumiStack {

	public static void main(String[] args) {
		Pulumi.run(PulumiStack::stack);
	}
    public static void stack(Context ctx) {
        final var config = ctx.config();

        final var ref = new StackReference(config.require("cluster-stack-reference"));

        final var bucketName = ref.getOutput("bucketName")
            .applyValue(String::valueOf);
        final var kubeconfig = ref.getOutput("kubeconfig")
            .applyValue(String::valueOf);

        // "target/spring-boot-complete-0.0.1-SNAPSHOT.war"
        final var jarPath = config.require("jar-path");

        final var deployment = new JarDeployment("my-deployment", args -> {
            args.appPort(Output.of(8080))
                .bucketName(bucketName)
                .kubeconfig(kubeconfig)
                .jarPath(jarPath);
        });

        ctx.export("endpoint", Output.format("http://%s/", deployment.endpoint));
    }

}
