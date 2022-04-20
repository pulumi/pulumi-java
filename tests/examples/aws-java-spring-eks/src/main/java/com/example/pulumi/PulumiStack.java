package com.example.pulumi;

import com.pulumi.Config;
import com.pulumi.Context;
import com.pulumi.Exports;
import com.pulumi.Pulumi;
import com.pulumi.aws.s3.BucketObject;
import com.pulumi.aws.s3.BucketObjectArgs;
import com.pulumi.core.Output;
import com.pulumi.kubernetes.Provider;
import com.pulumi.kubernetes.ProviderArgs;
import com.pulumi.kubernetes.apps_v1.Deployment;
import com.pulumi.kubernetes.apps_v1.DeploymentArgs;
import com.pulumi.kubernetes.apps_v1.inputs.DeploymentSpecArgs;
import com.pulumi.kubernetes.core_v1.*;
import com.pulumi.kubernetes.core_v1.ServiceArgs;
import com.pulumi.kubernetes.core_v1.inputs.*;
import com.pulumi.kubernetes.core_v1.enums.*;
import com.pulumi.kubernetes.core_v1.NamespaceArgs;
import com.pulumi.kubernetes.meta_v1.inputs.LabelSelectorArgs;
import com.pulumi.kubernetes.meta_v1.inputs.ObjectMetaArgs;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.StackReference;
import com.pulumi.resources.StackReferenceArgs;
import com.pulumi.asset.FileAsset;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class PulumiStack {

	public static void main(String[] args) {
		System.exit(Pulumi.run(PulumiStack::stack));
	}
    public static Exports stack(Context ctx) {
        final var config = ctx.config();
        //"target/spring-boot-complete-0.0.1-SNAPSHOT.war"

        final var stackRefId = Output.of(config.require("cluster-stack-reference"));
        final var jarPath = config.require("jar-path");

        final var ref0 = new StackReference(String.format("ref-%d", 0), new StackReferenceArgs(stackRefId), null);
        final var ref1 = new StackReference(String.format("ref-%d", 1), new StackReferenceArgs(stackRefId), null);

        final var bucketName = ref0.getOutput(Output.of("bucketName")).applyValue(String::valueOf);
        final var kubeconfig = ref1.getOutput(Output.of("kubeconfig")).applyValue(String::valueOf);

        final var deployment = new JarDeployment("my-deployment", args -> {
            args.appPort(Output.of(8080))
                .bucketName(bucketName)
                .kubeconfig(kubeconfig)
                .jarPath(jarPath);
        });
        return ctx.exports();
    }

}
