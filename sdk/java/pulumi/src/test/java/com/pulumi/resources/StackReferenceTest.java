package com.pulumi.resources;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.test.Mocks;
import com.pulumi.test.internal.PulumiTestInternal;

public class StackReferenceTest {
    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testOutputDetails() {
        var test = PulumiTestInternal.builder()
                .mocks(new OutputDetailsMock())
                .build();

        var result = test.runTest(ctx -> {
            var stackRef = new StackReference("myorg/mystack", StackReferenceArgs.Empty);

            stackRef.outputDetailsAsync("bucket").thenAccept(bucket -> {
                assertThat(bucket.getValue()).contains("mybucket-1234");
                assertThat(bucket.getSecretValue()).isEmpty();
            }).join();

            stackRef.outputDetailsAsync("password").thenAccept(password -> {
                assertThat(password.getValue()).isEmpty();
                assertThat(password.getSecretValue()).contains("secret-password");
            }).join();

            stackRef.outputDetailsAsync("unknown").thenAccept(unknown -> {
                assertThat(unknown.getSecretValue()).isEmpty();
                assertThat(unknown.getValue()).isEmpty();
            }).join();
        }).throwOnError();
        assertThat(result.exitCode()).isEqualTo(0);
    }

    static class OutputDetailsMock implements Mocks {
        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            if (args.type != "pulumi:pulumi:StackReference") {
                throw new IllegalArgumentException("Unexpected resource type: " + args.type);
            }

            ResourceResult result = ResourceResult.of(
                    Optional.of(args.name + "_id"),
                    ImmutableMap.<String, Object>of(
                            "outputs", ImmutableMap.<String, Object>of(
                                    "bucket", "mybucket-1234",
                                    "password", Output.ofSecret("secret-password")),
                            "secretOutputNames", ImmutableList.of("password")));

            return CompletableFuture.completedFuture(result);
        }
    }
}
