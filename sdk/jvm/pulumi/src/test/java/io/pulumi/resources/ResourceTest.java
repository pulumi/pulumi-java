package io.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import io.pulumi.Stack;
import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.MockCallArgs;
import io.pulumi.deployment.MockResourceArgs;
import io.pulumi.deployment.Mocks;
import io.pulumi.internal.PulumiMock;
import io.pulumi.internal.TestRuntimeContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.pulumi.internal.PulumiMock.cleanupDeploymentMocks;
import static org.assertj.core.api.Assertions.assertThat;

public class ResourceTest {

    private static PulumiMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = PulumiMock.builder()
                .setRuntimeContext(TestRuntimeContext.builder().setPreview(false).build())
                .setMocks(new MyMocks())
                .buildSpyGlobalInstance();
    }

    @AfterAll
    static void cleanup() {
        cleanupDeploymentMocks();
    }

    @Test
    void testProviderPropagation() {
        var resources = mock.testAsyncOrThrow(MyStack::new).join();

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
