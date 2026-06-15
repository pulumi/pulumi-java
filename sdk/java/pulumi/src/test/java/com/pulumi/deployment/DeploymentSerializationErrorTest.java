package com.pulumi.deployment;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.test.Mocks;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.pulumi.test.internal.PulumiTestInternal.logger;
import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentSerializationErrorTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void registerResourceMarshalErrorIsLoggedWithPropertyName() {
        var test = PulumiTestInternal.builder()
                .mocks(new EmptyMocks())
                .standardLogger(logger(Level.OFF))
                .build();

        var result = test.runTest(ctx -> {
            // The `badProp` input resolves to a value the serializer cannot handle, so marshalling the
            // resource inputs fails inside the serializer.
            var args = new BadResourceArgs(Output.of(new Unserializable()));
            new BadResource("res", args, null);
        });

        assertThat(result.errors())
                .as("expected the failing property name in the logged errors: %s", result.errors())
                .anyMatch(message -> message.contains("serializing property \"badProp\""));
    }

    // A type the Serializer does not know how to serialize.
    static final class Unserializable {
    }

    public static class BadResource extends CustomResource {
        public BadResource(String name, BadResourceArgs args, @Nullable CustomResourceOptions options) {
            super("test:index:BadResource", name, args, options);
        }
    }

    public static final class BadResourceArgs extends ResourceArgs {
        @Import(name = "badProp")
        @Nullable
        public final Output<Object> badProp;

        public BadResourceArgs(@Nullable Output<Object> badProp) {
            this.badProp = badProp;
        }
    }

    // Serialization fails before any RPC is made, so the mock is never invoked.
    static class EmptyMocks implements Mocks {
        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            return CompletableFuture.completedFuture(ResourceResult.of(Optional.of("id"), Map.of()));
        }
    }
}
