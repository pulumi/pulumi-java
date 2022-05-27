package com.pulumi.deployment;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.OutputTests;
import com.pulumi.core.Tuples;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Constructor;
import com.pulumi.core.annotations.CustomType.Parameter;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.InMemoryLogger;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.internal.StackDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.pulumi.core.OutputTests.waitForValue;
import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class MocksTest {

    @AfterEach
    public void printInternalErrorCount() {
        cleanupDeploymentMocks();
    }

    @Test
    void testCustomMocks() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new MyMocks())
                .build();

        var result = mock.runTestAsync(MocksTest::myStack).join()
                .throwOnError();

        var instance = result.resources.stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isPresent();

        var ip = OutputTests.waitFor(instance.get().publicIp);
        assertThat(ip.getValueNullable()).isEqualTo("203.0.113.12");
        assertThat(ip.isKnown()).isTrue();
        assertThat(ip.isSecret()).isFalse();
        assertThat(ip.getResources()).contains(instance.get()).hasSize(1);

        var id = OutputTests.waitFor(instance.get().getId());
        assertThat(id.getValueNullable()).isEqualTo("i-1234567890abcdef0");
        assertThat(id.isKnown()).isTrue();
        assertThat(id.isSecret()).isFalse();
        assertThat(id.getResources()).contains(instance.get()).hasSize(1);

        var urn = OutputTests.waitFor(instance.get().getUrn());
        assertThat(urn.getValueNullable()).isEqualTo(
                "urn:pulumi:stack::project::pulumi:pulumi:Stack$aws:ec2/instance:Instance::instance"
        );
        assertThat(urn.isKnown()).isTrue();
        assertThat(urn.isSecret()).isFalse();
        assertThat(urn.getResources()).contains(instance.get()).hasSize(1);
    }

    @Test
    void testCustomWithResourceReference() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new MyMocks())
                .build();

        var result = mock.runTestAsync(MocksTest::myStack).join()
                .throwOnError();

        var myCustom = result.resources.stream()
                .filter(r -> r instanceof MyCustom)
                .map(r -> (MyCustom) r)
                .findFirst();
        assertThat(myCustom).isPresent();

        var instance = OutputTests.waitFor(myCustom.get().instance).getValueNullable();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(Instance.class);

        var ip = OutputTests.waitFor(instance.publicIp).getValueNullable();
        assertThat(ip).isEqualTo("203.0.113.12");
    }

    @Test
    void testStack() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new MyMocks())
                .build();

        var result = mock.runTestAsync(MocksTest::myStack).join();
        var publicIp = waitForValue(result.output("publicIp", String.class));
        assertThat(publicIp).isEqualTo("203.0.113.12");
    }

    // Test inspired by https://github.com/pulumi/pulumi/issues/8163
    @Test
    void testInvokeThrowing() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new ThrowingMocks())
                .build();

        mock.standardLogger.setLevel(Level.OFF);

        var result = mock.runTestAsync(ctx -> {
            var invokeResult = mock.deployment.invokeAsync(
                    "aws:iam/getRole:getRole",
                    TypeShape.of(GetRoleResult.class),
                    new GetRoleArgs("doesNotExistTypoEcsTaskExecutionRole")
            );

            var publicIp = Output.of(invokeResult.thenApply(__ -> {
                var myInstance = new Instance("instance", new InstanceArgs(),
                        CustomResourceOptions.builder()
                                .build());
                return myInstance.publicIp;
            })).apply(Functions.identity());

            ctx.export("result", Output.of("x"));
            ctx.export("instance", publicIp);
        }).join();

        var resources = result.resources;
        var exceptions = result.exceptions;
        var errors = result.errors;

        assertThat(resources).isNotEmpty();
        assertThat(exceptions).isNotEmpty();
        assertThat(errors).isNotEmpty();

        var stack = resources.stream()
                .filter(r -> r instanceof StackDefinition)
                .map(r -> (StackDefinition) r)
                .findFirst();
        assertThat(stack).isPresent();

        var instance = resources.stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isNotPresent();

        assertThat(errors).hasSize(1);
        var error = errors.stream().findFirst().get();
        assertThat(error).startsWith("Running program [");
        assertThat(error).contains("failed with an unhandled exception:");
        assertThat(error).contains("io.grpc.StatusRuntimeException: UNKNOWN: error code 404");

        assertThat(exceptions).hasSize(2);
        assertThat(exceptions.get(0)).isInstanceOf(CompletionException.class);
        assertThat(exceptions.get(1)).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void testStackWithInvalidSchema() {
        var log = InMemoryLogger.getLogger("MocksTest#testStackWithInvalidSchema");
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new MyInvalidMocks())
                .setStandardLogger(log)
                .build();

        var result = mock.runTestAsync(MocksTest::myStack).join();
        var resources = result.resources;
        assertThat(resources).isNotEmpty();

        var exceptions = result.exceptions;
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.stream().map(Throwable::getMessage).collect(Collectors.toList()))
                .haveAtLeastOne(containsString("Instance.publicIp; Expected 'java.lang.String' but got 'java.lang.Double' while deserializing."));

        var publicIp = result.output("publicIp");
        var ipFuture = Internal.of(publicIp).getDataAsync();
        assertThat(ipFuture).isCompletedExceptionally();

        // Wait for all exceptions to propagate. If we do not, these exceptions contaminate the next test.
        // TODO properly isolate tests.
        CompletableFuture.runAsync(() -> { /* Empty */ }, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)).join();
    }

    @ResourceType(type = "aws:ec2/instance:Instance")
    public static class Instance extends CustomResource {
        @Export(type = String.class)
        public Output<String> publicIp;

        public Instance(String name, InstanceArgs args, @Nullable CustomResourceOptions options) {
            super("aws:ec2/instance:Instance", name, args, options);
        }
    }

    public static final class InstanceArgs extends ResourceArgs {
        /* Empty */
    }

    public static class MyCustom extends CustomResource {
        @Export(type = Instance.class)
        public Output<Instance> instance;

        public MyCustom(String name, MyCustomArgs args, @Nullable CustomResourceOptions options) {
            super("pkg:index:MyCustom", name, args, options);
        }
    }

    public static final class MyCustomArgs extends ResourceArgs {
        @Import
        @Nullable
        public final Output<Instance> instance;

        public MyCustomArgs(@Nullable Instance instance) {
            this.instance = Output.of(instance);
        }
    }

    public static final class GetRoleArgs extends InvokeArgs {
        /**
         * The friendly IAM role name to match.
         */
        @Import(required = true)
        public final String name;

        public GetRoleArgs(String name) {
            this.name = requireNonNull(name);
        }
    }

    @CustomType
    public static final class GetRoleResult {
        /**
         * The Amazon Resource Name (ARN) specifying the role.
         */
        public final String arn;
        public final String id;

        @Constructor
        private GetRoleResult(@Parameter("arn") String arn, @Parameter("id") String id) {
            this.arn = arn;
            this.id = id;
        }
    }

    public static void myStack(Context ctx) {
        var myInstance = new Instance("instance", new InstanceArgs(), null);
        //noinspection unused
        var res = new MyCustom("mycustom", new MyCustomArgs(myInstance), null);
        ctx.export("publicIp", myInstance.publicIp);
    }

    public static class MyMocks implements Mocks {

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "aws:ec2/instance:Instance":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.of("i-1234567890abcdef0"), ImmutableMap.of("publicIp", "203.0.113.12"))
                    );
                case "pkg:index:MyCustom":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.of(args.name + "_id"), args.inputs)
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null);
        }
    }

    public static class ThrowingMocks implements Mocks {

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            throw new RuntimeException("Not used");
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription("error code 404").asRuntimeException();
        }
    }

    public static class MyInvalidMocks implements Mocks {

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "aws:ec2/instance:Instance":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.of("i-1234567890abcdef0"), ImmutableMap.of("publicIp", 0xcb00710c))
                    );
                case "pkg:index:MyCustom":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.of(args.name + "_id"), args.inputs)
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
