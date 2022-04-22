package com.example.pulumi;

import com.pulumi.Context;
import com.pulumi.Exports;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.resources.StackReference;
import com.pulumi.resources.StackReferenceArgs;

public class PulumiStack {

	public static void main(String[] args) {
		System.exit(Pulumi.run(PulumiStack::stack));
	}
    public static Exports stack(Context ctx) {
        final var config = ctx.config();

        final var stackRefId = Output.of(config.require("cluster-stack-reference"));

        final var ref0 = new StackReference("ref-0", new StackReferenceArgs(stackRefId), null);
        final var ref1 = new StackReference("ref-1", new StackReferenceArgs(stackRefId), null);

        final var bucketName = ref0.getOutput(Output.of("bucketName")).applyValue(String::valueOf);
        final var kubeconfig = ref1.getOutput(Output.of("kubeconfig")).applyValue(String::valueOf);

        //"target/spring-boot-complete-0.0.1-SNAPSHOT.war"
        final var jarPath = config.require("jar-path");

        final var deployment = new JarDeployment("my-deployment", args -> {
            args.appPort(Output.of(8080))
                .bucketName(bucketName)
                .kubeconfig(kubeconfig)
                .jarPath(jarPath);
        });

        ctx.export("endpoint", deployment.endpoint);
        return ctx.exports();
    }

}
