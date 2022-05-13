package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.test.mock.MockCallArgs;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.resources.Stack;
import com.pulumi.test.PulumiTest;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.mock.MonitorMocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ConstructorConfusionTest {

    @Test
    void TryConstructNotConfusedByTwoN3Constructors() {
        var mock = PulumiTestInternal.withOptions(new TestOptions(true))
                .mocks(new ConfusionMocks())
                .useRealRunner()
                .build();
        var result = mock.runTestAsync(ctx ->
                ResourcePackages.tryConstruct(
                        "test:index/MinifiedConfigMap", "0.0.1",
                        "urn:pulumi:stack::project::test:index/MinifiedConfigMap::name"
                )
        ).join();
        assertThat(result.resources()).isNotEmpty();
    }

    @AfterEach
    public void cleanupMocks() {
        PulumiTest.cleanup();
    }

    public static class ConfusionMocks implements MonitorMocks {
        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "test:index/MinifiedConfigMap":
                    return CompletableFuture.completedFuture(new ResourceResult(Optional.of("i-1234567890abcdef0"), ImmutableMap.of()));
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null);
        }
    }

    @SuppressWarnings("unused") // will be used by reflection
    @ResourceType(type = "test:index/MinifiedConfigMap")
    public static class MinifiedConfigMap extends CustomResource {
        public MinifiedConfigMap(String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super("test:index/MinifiedConfigMap", name, ResourceArgs.Empty, CustomResourceOptions.Empty);
        }

        private MinifiedConfigMap(String name, Output<String> id, @Nullable CustomResourceOptions options) {
            super("test:index/MinifiedConfigMap", name, null, options);
        }
    }

    @SuppressWarnings("unused") // will be used by reflection
    public static class ConfusionStack extends Stack {
        public ConfusionStack() {
            ResourcePackages.tryConstruct("test:index/MinifiedConfigMap", "0.0.1", "urn:pulumi:stack::project::test:index/MinifiedConfigMap::name");
        }
    }
}
