package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.Tuples;
import com.pulumi.core.Tuples.Tuple2;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.deployment.MockCallArgs;
import com.pulumi.deployment.MockResourceArgs;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class ConstructorConfusionTest {

    @AfterEach
    public void cleanupMocks() {
        DeploymentTests.cleanupDeploymentMocks();
    }

    @Test
    void tryConstructNotConfusedByTwoN3Constructors() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new ConfusionMocks())
                .build();
        var resourcePackages = new ResourcePackages(Log.ignore());
        var result = mock.runTestAsync(
                        context -> resourcePackages.tryConstruct(
                                "test:index/MinifiedConfigMap",
                                "0.0.1",
                                "urn:pulumi:stack::project::test:index/MinifiedConfigMap::name"
                        )
                ).join()
                .throwOnError();
        assertThat(result.resources).isNotEmpty();
    }

    public static class ConfusionMocks implements Mocks {
        @Override
        public CompletableFuture<Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "test:index/MinifiedConfigMap":
                    return CompletableFuture.completedFuture(Tuples.of(Optional.of("i-1234567890abcdef0"), ImmutableMap.of()));
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.completedFuture(null);
        }
    }

    @SuppressWarnings("unused") // Used with reflection
    @ResourceType(type = "test:index/MinifiedConfigMap")
    public static class MinifiedConfigMap extends CustomResource {
        public MinifiedConfigMap(String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super("test:index/MinifiedConfigMap", name, ResourceArgs.Empty, CustomResourceOptions.Empty);
        }

        private MinifiedConfigMap(String name, Output<String> id, @Nullable CustomResourceOptions options) {
            super("test:index/MinifiedConfigMap", name, null, options);
        }
    }
}
