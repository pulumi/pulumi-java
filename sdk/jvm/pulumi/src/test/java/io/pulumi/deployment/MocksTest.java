package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import io.grpc.Status;
import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import io.pulumi.core.annotations.CustomType;
import io.pulumi.core.annotations.CustomType.Constructor;
import io.pulumi.core.annotations.CustomType.Parameter;
import io.pulumi.core.annotations.Export;
import io.pulumi.core.annotations.Import;
import io.pulumi.core.annotations.ResourceType;
import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.InMemoryLogger;
import io.pulumi.deployment.internal.TestOptions;
import io.pulumi.resources.CustomResource;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.resources.InvokeArgs;
import io.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.pulumi.core.TypeShape.of;
import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static io.pulumi.test.internal.assertj.PulumiConditions.containsString;
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
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack::new).join();

        var instance = resources.stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isPresent();

        var ip = OutputTests.waitFor(instance.get().publicIp).getValueNullable();
        assertThat(ip).isEqualTo("203.0.113.12");
    }

    @Test
    void testCustomWithResourceReference() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new MyMocks())
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack::new).join();

        var myCustom = resources.stream()
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
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack::new).join();

        var stack = resources.stream()
                .filter(r -> r instanceof MyStack)
                .map(r -> (MyStack) r)
                .findFirst();
        assertThat(stack).isPresent();

        var ip = OutputTests.waitFor(stack.get().publicIp).getValueNullable();
        assertThat(ip).isEqualTo("203.0.113.12");
    }

    // Test inspired by https://github.com/pulumi/pulumi/issues/8163
    @Test
    void testInvokeThrowing() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new ThrowingMocks())
                .setSpyGlobalInstance();

        mock.standardLogger.setLevel(Level.OFF);

        var result = mock.runAsync(
                () -> mock.deployment.invokeAsync(
                        "aws:iam/getRole:getRole",
                        of(GetRoleResult.class), new GetRoleArgs("doesNotExistTypoEcsTaskExecutionRole")
                ).thenApply(__ -> {
                    var myInstance = new Instance("instance", new InstanceArgs(), null);

                    return ImmutableMap.<String, Output<?>>builder()
                            .put("result", Output.of("x"))
                            .put("instance", Output.of(myInstance.publicIp))
                            .build();
                })).join();

        var resources = result.resources;
        var exceptions = result.exceptions;

        assertThat(resources).isNotEmpty();
        assertThat(exceptions).isNotEmpty();

        var stack = resources.stream()
                .filter(r -> r instanceof Stack)
                .map(r -> (Stack) r)
                .findFirst();
        assertThat(stack).isPresent();

        var instance = resources.stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isNotPresent();

        assertThat(exceptions).hasSize(1);
        var exception = exceptions.stream().findFirst().get();

        assertThat(exception).hasMessageStartingWith("Running program [");
        assertThat(exception).hasMessageContaining("failed with an unhandled exception:");
        assertThat(exception).hasMessageContaining("io.grpc.StatusRuntimeException: UNKNOWN: error code 404");
    }

    @Test
    void testStackWithInvalidSchema() {
        var log = InMemoryLogger.getLogger("MocksTest#testStackWithInvalidSchema");
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new MyInvalidMocks())
                .setStandardLogger(log)
                .setSpyGlobalInstance();

        var result = mock.tryTestAsync(MyStack::new).join();
        var resources = result.resources;
        assertThat(resources).isNotEmpty();

        var exceptions = result.exceptions;
        assertThat(exceptions).isNotEmpty();
        assertThat(exceptions.stream().map(Throwable::getMessage).collect(Collectors.toList()))
                .haveAtLeastOne(containsString("Instance.publicIp; Expected 'java.lang.String' but got 'java.lang.Double' while deserializing."));

        var stack = resources.stream()
                .filter(r -> r instanceof MyStack)
                .map(r -> (MyStack) r)
                .findFirst();
        assertThat(stack).isPresent();

        var ipFuture = Internal.of(stack.get().publicIp).getDataAsync();
        assertThat(ipFuture).isCompletedExceptionally();

        // Wait for all exceptions to propagate. If we do not, these exceptions contaminate the next test.
        // TODO properly isolate tests.
        CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)).join();
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

    public static class MyStack extends Stack {
        @Export(type = String.class)
        public final Output<String> publicIp;

        public MyStack() {
            var myInstance = new Instance("instance", new InstanceArgs(), null);
            //noinspection unused
            var res = new MyCustom("mycustom", new MyCustomArgs(myInstance), null);
            this.publicIp = myInstance.publicIp;
        }
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
