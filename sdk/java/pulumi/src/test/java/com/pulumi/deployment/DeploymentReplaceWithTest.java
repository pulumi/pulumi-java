package com.pulumi.deployment;

import com.pulumi.Context;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentReplaceWithTest {

    private static PulumiTestInternal test;

    @BeforeAll
    public static void mockSetup() {
        test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(false).build())
                .mocks(new MyReplaceWithMocks())
                .build();
    }

    @AfterAll
    static void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testReplaceWithResourcesAreTracked() {
        var result = test.runTest(ReplaceWithStack::init)
                .throwOnError();

        var resources = result.resources();

        var target = resources.stream()
                .filter(r -> r instanceof MyResource)
                .map(r -> (MyResource) r)
                .filter(r -> "target".equals(r.pulumiResourceName()))
                .findFirst();

        var replaceWith = resources.stream()
                .filter(r -> r instanceof MyResource)
                .map(r -> (MyResource) r)
                .filter(r -> "replaceWith".equals(r.pulumiResourceName()))
                .findFirst();

        var notReplaceWith = resources.stream()
                .filter(r -> r instanceof MyResource)
                .map(r -> (MyResource) r)
                .filter(r -> "notReplaceWith".equals(r.pulumiResourceName()))
                .findFirst();

        assertThat(target).isPresent();
        assertThat(replaceWith).isPresent();
        assertThat(notReplaceWith).isPresent();
    }

    public static class ReplaceWithStack {
        public static void init(Context ctx) {
            var target = new MyResource("target", null, null);

            // Resource that is replaced with the target resource.
            var replaceWith = new MyResource("replaceWith", null, CustomResourceOptions.builder()
                    .replaceWith(target)
                    .build());

            // Resource that is not replaced with any resource.
            var notReplaceWith = new MyResource("notReplaceWith", null, CustomResourceOptions.builder()
                    .build());

            // Make sure the resources are not considered unused.
            ctx.export("targetName", target.urn());
            ctx.export("replaceWithName", replaceWith.urn());
            ctx.export("notReplaceWithName", notReplaceWith.urn());
        }
    }

    public static final class MyArgs extends ResourceArgs {
        // Empty
    }

    @ResourceType(type = "test:DeploymentReplaceWith:resource")
    public static class MyResource extends CustomResource {
        public MyResource(String name, @Nullable MyArgs args, @Nullable CustomResourceOptions options) {
            super("test:DeploymentReplaceWith:resource", name, args == null ? new MyArgs() : args, options);
        }
    }

    static class MyReplaceWithMocks implements Mocks {
        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            Objects.requireNonNull(args.type);

            if ("test:DeploymentReplaceWith:resource".equals(args.type)) {
                // We do not care about state here, just that the resources can be created successfully.
                return CompletableFuture.completedFuture(
                        ResourceResult.of(Optional.of(args.name + "_id"), Map.of())
                );
            }

            throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
        }
    }
}


