package io.pulumi.deployment;

import com.google.common.collect.ImmutableMap;
import io.pulumi.Stack;
import io.pulumi.core.OutputTests;
import io.pulumi.core.Tuples;
import io.pulumi.core.annotations.ResourceType;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.TestOptions;
import io.pulumi.resources.CustomResource;
import io.pulumi.resources.CustomResourceOptions;
import io.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentResourceDependencyGatheringTest {
    private static DeploymentTests.DeploymentMock mock;

    @BeforeAll
    public static void mockSetup() {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(true))
                .setMocks(new MyMocks(true))
                .setSpyGlobalInstance();
    }

    @Test
    void testDeploysResourcesWithUnknownDependsOn() {
        var result = mock.tryTestAsync(DeploysResourcesWithUnknownDependsOnStack.class).join();
        assertThat(result.exceptions).isNotNull();
        assertThat(result.exceptions).isEmpty();
    }

    public static class DeploysResourcesWithUnknownDependsOnStack extends Stack {
        public DeploysResourcesWithUnknownDependsOnStack() {
            var r = new MyCustomResource("r1", null, CustomResourceOptions.builder()
                    .dependsOn(OutputTests.unknown())
                    .build()
            );
        }
    }

    public static final class MyArgs extends ResourceArgs {
    }

    @ResourceType(type = "test:DeploymentResourceDependencyGatheringTests:resource")
    private static class MyCustomResource extends CustomResource {
        public MyCustomResource(String name, @Nullable MyArgs args, @Nullable CustomResourceOptions options) {
            super("test:DeploymentResourceDependencyGatheringTests:resource", name, args == null ? new MyArgs() : args, options);
        }
    }

    static class MyMocks implements Mocks {
        private final boolean isPreview;

        MyMocks(boolean isPreview) {
            this.isPreview = isPreview;
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            throw new IllegalStateException(String.format("Unknown function %s", args.token));
        }

        @Override
        public CompletableFuture<Tuples.Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            Objects.requireNonNull(args.type);
            if ("test:DeploymentResourceDependencyGatheringTests:resource".equals(args.type)) {
                return CompletableFuture.completedFuture(
                        Tuples.of(Optional.ofNullable(this.isPreview ? null : "id"), ImmutableMap.<String, Object>of())
                );
            }
            throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
        }
    }
}
