package com.pulumi.deployment;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.CustomType;
import com.pulumi.core.annotations.CustomType.Setter;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.ContextAwareCompletableFuture;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.InMemoryLogger;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.internal.Stack;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
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

import static com.pulumi.test.PulumiTest.extractValue;
import static com.pulumi.test.internal.PulumiTestInternal.extractOutputData;
import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static com.pulumi.test.internal.assertj.PulumiConditions.containsString;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class MocksTest {

    @AfterEach
    public void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testCustomMocks() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(true).build())
                .mocks(new MyMocks())
                .build();

        var result = test.runTest(MocksTest::myStack)
                .throwOnError();

        var instance = result.resources().stream()
                .filter(r -> r instanceof Instance)
                .map(r -> (Instance) r)
                .findFirst();
        assertThat(instance).isPresent();

        var ip = extractOutputData(instance.get().publicIp);
        assertThat(ip.getValueNullable()).isEqualTo("203.0.113.12");
        assertThat(ip.isKnown()).isTrue();
        assertThat(ip.isSecret()).isFalse();
        assertThat(ip.getResources()).contains(instance.get()).hasSize(1);

        var id = extractOutputData(instance.get().id());
        assertThat(id.getValueNullable()).isEqualTo("i-1234567890abcdef0");
        assertThat(id.isKnown()).isTrue();
        assertThat(id.isSecret()).isFalse();
        assertThat(id.getResources()).contains(instance.get()).hasSize(1);

        var urn = extractOutputData(instance.get().urn());
        assertThat(urn.getValueNullable()).isEqualTo(
                "urn:pulumi:stack::project::pulumi:pulumi:Stack$aws:ec2/instance:Instance::instance"
        );
        assertThat(urn.isKnown()).isTrue();
        assertThat(urn.isSecret()).isFalse();
        assertThat(urn.getResources()).contains(instance.get()).hasSize(1);
    }

    @Test
    void testCustomWithResourceReference() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(false).build())
                .mocks(new MyMocks())
                .build();

        var result = test.runTest(MocksTest::myStack)
                .throwOnError();

        var myCustom = result.resources().stream()
                .filter(r -> r instanceof MyCustom)
                .map(r -> (MyCustom) r)
                .findFirst();
        assertThat(myCustom).isPresent();

        var instance = extractValue(myCustom.get().instance);
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(Instance.class);

        var ip = extractValue(instance.publicIp);
        assertThat(ip).isEqualTo("203.0.113.12");
    }

    @Test
    void testStack() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(true).build())
                .mocks(new MyMocks())
                .build();

        var result = test.runTest(MocksTest::myStack);
        var publicIp = extractValue(result.output("publicIp", String.class));
        assertThat(publicIp).isEqualTo("203.0.113.12");
    }

    // Test inspired by https://github.com/pulumi/pulumi/issues/8163
    @Test
    void testInvokeThrowing() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(false).build())
                .mocks(new ThrowingMocks())
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTest(ctx -> {
            var invokeResult = Deployment.getInstance().invokeAsync(
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
        });

        var resources = result.resources();
        var exceptions = result.exceptions();
        var errors = result.errors();

        assertThat(resources).isNotEmpty();
        assertThat(exceptions).isNotEmpty();
        assertThat(errors).isNotEmpty();

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
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(false).build())
                .mocks(new MyInvalidMocks())
                .standardLogger(log)
                .build();

        var result = test.runTest(MocksTest::myStack);
        var resources = result.resources();
        assertThat(resources).isNotEmpty();

        var exceptions = result.exceptions();
        assertThat(exceptions).isEmpty();

        assertThat(log.getMessages()).haveAtLeastOne(containsString(
                "Instance.publicIp; Expected 'java.lang.String' but got 'java.lang.Double' while deserializing."
        ));

        var publicIp = result.output("publicIp");
        var ipFuture = Internal.of(publicIp).getDataAsync();
        assertThat(ipFuture).isCompleted();

        // Wait for all exceptions to propagate. If we do not, these exceptions contaminate the next test.
        // TODO properly isolate tests.
        ContextAwareCompletableFuture.runAsync(() -> { /* Empty */ }, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)).join();
    }

    @ResourceType(type = "aws:ec2/instance:Instance")
    public static class Instance extends CustomResource {
        @Export(refs = String.class)
        public Output<String> publicIp;

        public Instance(String name, InstanceArgs args, @Nullable CustomResourceOptions options) {
            super("aws:ec2/instance:Instance", name, args, options);
        }
    }

    public static final class InstanceArgs extends ResourceArgs {
        /* Empty */
    }

    public static class MyCustom extends CustomResource {
        @Export(refs = Instance.class)
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
        private @Nullable String arn;
        private @Nullable String id;

        /**
         * The Amazon Resource Name (ARN) specifying the role.
         */
        @Nullable
        public String arn() {
            return arn;
        }

        @Nullable
        public String id() {
            return id;
        }

        @CustomType.Builder
        private static final class Builder {
            private final GetRoleResult $;

            private Builder() {
                this.$ = new GetRoleResult();
            }

            private Builder(GetRoleResult defaults) {
                this.$ = defaults;
            }

            @Setter("arn")
            private Builder arn(@Nullable String arn) {
                this.$.arn = arn;
                return this;
            }

            @Setter("id")
            private Builder id(@Nullable String id) {
                this.$.id = id;
                return this;
            }

            private GetRoleResult build() {
                return this.$;
            }
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
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "aws:ec2/instance:Instance":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.of("i-1234567890abcdef0"), ImmutableMap.of("publicIp", "203.0.113.12"))
                    );
                case "pkg:index:MyCustom":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.of(args.name + "_id"), args.inputs)
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }

    public static class ThrowingMocks implements Mocks {

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            throw new RuntimeException("Not used");
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(CallArgs args) {
            throw Status.fromCode(Status.Code.UNKNOWN).withDescription("error code 404").asRuntimeException();
        }
    }

    public static class MyInvalidMocks implements Mocks {

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "aws:ec2/instance:Instance":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.of("i-1234567890abcdef0"), ImmutableMap.of("publicIp", 0xcb00710c))
                    );
                case "pkg:index:MyCustom":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.of(args.name + "_id"), args.inputs)
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }
}
