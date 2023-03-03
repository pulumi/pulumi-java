package com.pulumi.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import com.pulumi.test.Mocks;
import com.pulumi.test.internal.PulumiTestInternal;

// https://github.com/pulumi/pulumi/issues/12161
public class ComponentResourceTest {
    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }

    /**
     * Tests that a component resource propagates the "provider" option to its children
     * even though it itself cannot use the provider.
     */
    @Test
    void testPropagatesProvider() {
        ProviderCaptureMocks mocks = new ProviderCaptureMocks();

        var test = PulumiTestInternal.builder().mocks(mocks).build();
        test.runTest(ctx -> {
            var provider = new ProviderResource("test", "prov", null, null);

            // The ComponentResource cannot use the provider.
            var component = new ComponentResource("custom:foo:Component", "comp",
                    ComponentResourceOptions.builder().provider(provider).build());

            // The CustomResource can use the provider.
            // It should inherit the provider from the ComponentResource.
            new CustomResource("test:index:Resource", "custom", null,
                    CustomResourceOptions.builder().parent(component).build());
        }).throwOnError();

        String urn = mocks.providers.get("custom");
        assertThat(urn).contains("test::prov::prov_id");
    }

    /**
     * Tests that a component resource propagates the "providers" option to its children
     * even though it itself cannot use the provider.
     */
    @Test
    void testPropagatesProvidersList() {
        ProviderCaptureMocks mocks = new ProviderCaptureMocks();

        var test = PulumiTestInternal.builder().mocks(mocks).build();
        test.runTest(ctx -> {
            var provider = new ProviderResource("test", "prov", null, null);

            // The ComponentResource cannot use the provider.
            var component = new ComponentResource("custom:foo:Component", "comp",
                    ComponentResourceOptions.builder().providers(provider).build());

            // The CustomResource can use the provider.
            // It should inherit the provider from the ComponentResource.
            new CustomResource("test:index:Resource", "custom", null,
                    CustomResourceOptions.builder().parent(component).build());
        }).throwOnError();

        String urn = mocks.providers.get("custom");
        assertThat(urn).contains("test::prov::prov_id");
    }

    static class ProviderCaptureMocks implements Mocks {
        Map<String, String> providers = new HashMap<>();

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            this.providers.put(args.name, args.provider);

            var result = ResourceResult.of(Optional.of(args.name + "_id"), args.inputs);
            return CompletableFuture.completedFuture(result);
        }
    }
}