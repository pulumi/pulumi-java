package io.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import io.pulumi.Stack;
import io.pulumi.core.InputOutputTests;
import io.pulumi.core.Tuples;
import io.pulumi.deployment.MockCallArgs;
import io.pulumi.deployment.MockMonitor;
import io.pulumi.deployment.MockResourceArgs;
import io.pulumi.deployment.Mocks;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.test.internal.TestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static io.pulumi.deployment.internal.DeploymentTests.printErrorCount;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceTest {

    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(false))
                .setMonitor(new MockMonitor(new MyMocks()))
                .setSpyGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @AfterEach
    public void printInternalErrorCount() {
        printErrorCount(mock.logger);
    }

    @Test
    void testProviderPropagation() {
        var resources = mock.testAsync(MyStack.class).join();

        var resource = resources.stream()
                .filter(r -> r.getResourceName().equals("testResource"))
                .findFirst()
                .orElse(null);
        assertThat(resource).isNotNull();

        var urn = InputOutputTests.waitFor(resource.getUrn()).getValueNullable();
        var provider = CustomResource.internalGetProvider(resource, resource.getResourceType());

        assertThat(provider).isNotNull();
        assertThat(provider.getResourceName()).isEqualTo("testProvider");
        assertThat(provider.getResourceType()).isEqualTo("pulumi:providers:test");
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
                            .setProvider(provider)
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
