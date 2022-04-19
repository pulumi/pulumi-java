package com.example.pulumi;

/*
import java.text.MessageFormat;
import java.util.Map;
import java.util.stream.Collectors;

import com.pulumi.*;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.core.*;
import com.pulumi.aws.eks.Cluster;
import com.pulumi.aws.eks.ClusterArgs;
import com.pulumi.aws.ec2.*;
 */

import com.pulumi.Context;
import com.pulumi.Exports;
import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.Ec2Functions;
import com.pulumi.aws.ec2.inputs.GetSubnetIdsArgs;
import com.pulumi.aws.ec2.inputs.GetVpcArgs;
import com.pulumi.aws.ec2.outputs.GetVpcResult;
import com.pulumi.aws.iam.*;
import com.pulumi.aws.iam.inputs.RoleInlinePolicyArgs;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.s3.BucketArgs;
import com.pulumi.aws.s3.BucketObject;
import com.pulumi.aws.s3.BucketObjectArgs;
import com.pulumi.core.Output;
import com.pulumi.eks.Cluster;
import com.pulumi.eks.ClusterArgs;
import com.pulumi.kubernetes.Provider;
import com.pulumi.kubernetes.ProviderArgs;
import com.pulumi.kubernetes.apps_v1.Deployment;
import com.pulumi.kubernetes.apps_v1.DeploymentArgs;
import com.pulumi.kubernetes.apps_v1.inputs.DeploymentSpecArgs;
import com.pulumi.kubernetes.core_v1.*;
import com.pulumi.kubernetes.core_v1.ConfigMapArgs;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PulumiStack {

	public static void main(String[] args) {
		System.exit(Pulumi.run(PulumiStack::stack));
	}

	private static CustomResourceOptions stack2(StackReference ref) {
		final var kubeconfig = ref.getOutput(Output.of("kubeconfig")).applyValue(String::valueOf);

		// Create a Kubernetes provider instance that uses our cluster from above.
		final var clusterProvider = new Provider("myProvider",
				ProviderArgs.builder()
						.kubeconfig(kubeconfig)
						.build());
		final var clusterResourceOptions = CustomResourceOptions.builder()
				.provider(clusterProvider)
				.build();
		return clusterResourceOptions;
	}

	public static Exports stack(Context ctx) {
		final var ref = new StackReference("ref1", new StackReferenceArgs(Output.of("dixler/eks-minimal/ved")), null);
		final var ref2 = new StackReference("ref2", new StackReferenceArgs(Output.of("dixler/eks-minimal/ved")), null);
		//final var ref2 = new StackReference("dixler/eks-minimal/ved", null, null);

		final var clusterResourceOptions = stack2(ref);
		final var jarBucket = ref2.getOutput(Output.of("jarBucket")).applyValue(String::valueOf);
		ctx.export("imjar", jarBucket);

		// Create a Kubernetes Namespace
		final var namespace = new Namespace("ns-1", NamespaceArgs.builder()
				.build(),
				clusterResourceOptions);

		// Export the Namespace name
		final var appLabels = Map.of("appClass", "gs-spring-boot");

		final var metadata = ObjectMetaArgs.builder()
				.namespace(namespace.getId())
				.labels(appLabels)
				.build();

		final var file = new FileAsset("target/spring-boot-complete-0.0.1-SNAPSHOT.war");

		final var appJar = new BucketObject("jar", BucketObjectArgs.builder()
				.bucket(jarBucket)
				.source(file)
				.build());

		new Bucket("thing", BucketArgs.builder()
				.build());
		final var targetDir = "/var/run/secrets/java";
		// Create a Java deployment
		final var deployment = new Deployment("deployment", DeploymentArgs.builder()
				.metadata(metadata)
				.spec(DeploymentSpecArgs.builder()
						.replicas(1)
						.selector(LabelSelectorArgs.builder()
								.matchLabels(appLabels)
								.build())
						.template(PodTemplateSpecArgs.builder()
								.metadata(metadata)
								.spec(PodSpecArgs.builder()
										.volumes(VolumeArgs.builder()
												/*
												.name("config-volume")
												.configMap(ConfigMapVolumeSourceArgs.builder()
														.name(appMap.getId().applyValue(id -> id.split(Pattern.quote("/"))[1]))
														.build())
												 */
												.name("jar-volume")
												.emptyDir(EmptyDirVolumeSourceArgs.Empty)
												.build())
										.initContainers(ContainerArgs.builder()
												.name("init")
												.image("amazon/aws-cli")
												.command(
														Output.tuple(Output.format("s3://%s/%s", appJar.bucket(), appJar.key()), Output.of(targetDir))
															.applyValue(t -> List.of( "aws", "s3", "cp", t.t1, t.t2+"/app.jar")))
												.volumeMounts(
														VolumeMountArgs.builder()
																.name("jar-volume")
																.mountPath("/var/run/secrets/java")
																.build())
												.build())
										.containers(ContainerArgs.builder()
												.name("app")
												.image("openjdk")
												.command("java", "-jar", targetDir+"/app.jar")
												.volumeMounts(
														VolumeMountArgs.builder()
																.name("jar-volume")
																.mountPath("/var/run/secrets/java")
																.build())
												.ports(ContainerPortArgs.builder()
														.name("http")
														.containerPort(8080)
														.build())
												.build())
										.build())
								.build())
						.build())
				.build(), clusterResourceOptions);
		// Create a LoadBalancer Service for the Deployment
		final var service = new Service("app-svc", ServiceArgs.builder()
				.metadata(metadata)
				.spec(ServiceSpecArgs.builder()
						.type(Output.ofRight(ServiceSpecType.LoadBalancer))
						.ports(ServicePortArgs.builder()
								.port(80)
								.targetPort(Output.ofRight("http"))
								.build())
						.selector(appLabels)
						.build())
				.build(), clusterResourceOptions);

		// Export the Deployment name
		ctx.export("deploymentName", deployment.metadata().apply(m -> Output.of(m.name().orElseThrow())));

		return ctx.exports();
	}
}
