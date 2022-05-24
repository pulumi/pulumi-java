package com.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import com.pulumi.Context;
import com.pulumi.core.OutputTests;
import com.pulumi.core.Tuples;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.MockCallArgs;
import com.pulumi.deployment.MockResourceArgs;
import com.pulumi.deployment.Mocks;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.deployment.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceTest {

    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMocks(new MyMocks())
                .build();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testProviderPropagation() {
        var result = mock.runTestAsync(MyStack::init).join()
                .throwOnError();

        var resource = result.resources.stream()
                .filter(r -> r.getResourceName().equals("testResource"))
                .findFirst()
                .orElse(null);
        assertThat(resource).isNotNull();

        var urn = OutputTests.waitFor(resource.getUrn()).getValueNullable();
        var provider = Internal.from(resource).getProvider(resource.getResourceType());

        assertThat(provider).isPresent();
        assertThat(provider.get().getResourceName()).isEqualTo("testProvider");
        assertThat(provider.get().getResourceType()).isEqualTo("pulumi:providers:test");
    }

    public static class MyStack {
        public static void init(Context ctx) {
            var mod = "test";
            var provider = new ProviderResource(
                    mod, "testProvider", ResourceArgs.Empty, CustomResourceOptions.Empty
            );

            var resource = new CustomResource(
                    mod + ":a/b:c", "testResource", ResourceArgs.Empty,
                    CustomResourceOptions.builder()
                            .provider(provider)
                            .build()
            );
        }
    }

    static class MyMocks implements Mocks {

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            throw new IllegalStateException(String.format("Unknown function %s", args.token));
        }

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            Objects.requireNonNull(args.type);
            switch (args.type) {
                case "pulumi:providers:test":
                case "test:a/b:c":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.empty(), ImmutableMap.<String, Object>of())
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }
}
