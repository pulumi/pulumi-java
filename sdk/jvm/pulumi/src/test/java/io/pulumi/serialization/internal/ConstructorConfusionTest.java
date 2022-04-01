package io.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import io.pulumi.Stack;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.Tuples.Tuple2;
import io.pulumi.core.annotations.ResourceType;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.MockCallArgs;
import io.pulumi.deployment.MockResourceArgs;
import io.pulumi.deployment.Mocks;
import io.pulumi.deployment.internal.CurrentDeployment;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import io.pulumi.resources.CustomResource;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.resources.ResourceArgs;
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
    void TryConstuctNotConfusedByTwoN3Constructors() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new ConfusionMocks())
                .buildSpyInstance();
        var resources = mock.testAsync(ConfusionStack.class).join();
        assertThat(resources).isNotEmpty();
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

    @ResourceType(type = "test:index/MinifiedConfigMap")
    public static class MinifiedConfigMap extends CustomResource {
        public MinifiedConfigMap(String name, @Nullable ResourceArgs args, @Nullable CustomResourceOptions options) {
            super("test:index/MinifiedConfigMap", name, ResourceArgs.Empty, CustomResourceOptions.builder().build());
        }

        private MinifiedConfigMap(Deployment deployment, String name, Output<String> id, @Nullable CustomResourceOptions options) {
            super("test:index/MinifiedConfigMap", name, null, options);
        }
    }

    public static class ConfusionStack extends Stack {
        public ConfusionStack() {
            var deployment = CurrentDeployment.getCurrentDeploymentOrThrow();
            ResourcePackages.tryConstruct(deployment,"test:index/MinifiedConfigMap", "0.0.1", "urn:pulumi:stack::project::test:index/MinifiedConfigMap::name");
        }
    }
}
