package com.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import com.pulumi.core.OutputTests;
import com.pulumi.core.internal.Internal;
import com.pulumi.test.mock.MockCallArgs;
import com.pulumi.deployment.internal.DeploymentTests;
import com.pulumi.test.TestOptions;
import com.pulumi.test.mock.MonitorMocks;
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
                .setSpyGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testProviderPropagation() {
        var resources = mock.testAsync(MyStack::new).join();

        var resource = resources.stream()
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

    public static class MyStack extends Stack {
        public MyStack() {
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

    static class MyMocks implements MonitorMocks {

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            throw new IllegalStateException(String.format("Unknown function %s", args.token));
        }

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            Objects.requireNonNull(args.type);
            switch (args.type) {
                case "pulumi:providers:test":
                case "test:a/b:c":
                    return CompletableFuture.completedFuture(
                            new ResourceResult(Optional.empty(), ImmutableMap.<String, Object>of())
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }
}
