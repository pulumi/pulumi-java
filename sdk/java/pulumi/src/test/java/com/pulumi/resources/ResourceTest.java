package com.pulumi.resources;

import com.google.common.collect.ImmutableMap;
import com.pulumi.core.internal.Internal;
import com.pulumi.resources.internal.Stack;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pulumirpc.Resource.RegisterResourceRequest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.pulumi.test.PulumiTest.extractValue;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ResourceTest {

    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @Test
    void testProviderPropagation() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(false).build())
                .mocks(new ProviderPropagationMocks())
                .build();
        var result = test.runTest(ctx -> {
                    var mod = "test";
                    var provider = new ProviderResource(
                            mod, "testProvider", ResourceArgs.Empty, CustomResourceOptions.Empty
                    );

                    new CustomResource(
                            mod + ":a/b:c", "testResource", ResourceArgs.Empty,
                            CustomResourceOptions.builder()
                                    .provider(provider)
                                    .build()
                    );
                }).throwOnError();

        var resource = result.resources().stream()
                .filter(r -> r.pulumiResourceName().equals("testResource"))
                .findFirst()
                .orElse(null);
        assertThat(resource).isNotNull();

        var urn = extractValue(resource.urn());
        var provider = Internal.from(resource).getProvider(resource.pulumiResourceType());

        assertThat(provider).isPresent();
        assertThat(provider.get().pulumiResourceName()).isEqualTo("testProvider");
        assertThat(provider.get().pulumiResourceType()).isEqualTo("pulumi:providers:test");
    }

    @Test
    void testReplaceOnChanges() {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(false).build())
                .monitorDecorator(Mockito::spy)
                .mocks(new ReplaceOnChangesMocks())
                .build();

        var result = test.runTest(ctx ->
                new MyResource("testResource", CustomResourceOptions.builder().replaceOnChanges("foo").build())
        );

        assertThat(result.resources())
                .hasSize(2)
                .hasExactlyElementsOfTypes(Stack.class, MyResource.class);

        var resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        var requestCaptor = ArgumentCaptor.forClass(RegisterResourceRequest.class);
        verify(test.monitor(), times(2)).registerResourceAsync(resourceCaptor.capture(), requestCaptor.capture());

        assertThat(resourceCaptor.getAllValues().get(1)).isInstanceOf(MyResource.class);
        assertThat(requestCaptor.getAllValues().get(1).getReplaceOnChangesList())
                .containsExactly("foo");
    }

    static class ProviderPropagationMocks implements Mocks {

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "pulumi:providers:test":
                case "test:a/b:c":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.empty(), ImmutableMap.<String, Object>of())
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }

    static class MyResource extends CustomResource {
        public MyResource(String name, CustomResourceOptions options) {
            super("test:index:MyResource", name, ResourceArgs.Empty, options);
        }
    }

    static class ReplaceOnChangesMocks implements Mocks {

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            requireNonNull(args.type);
            switch (args.type) {
                case "test:index:MyResource":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.empty(), ImmutableMap.<String, Object>of())
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }
}
