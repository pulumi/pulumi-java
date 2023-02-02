package com.pulumi.deployment.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.pulumi.core.Alias;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.internal.AliasSerializer;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.test.Mocks;
import com.pulumi.test.TestOptions;
import com.pulumi.test.internal.PulumiTestInternal;
import com.pulumi.test.internal.PulumiTestInternal;

public class AliasSerializerTest {
    @Test
    void testSerializeWithUrnAlias() {
        final var urn = "urn:pulumi:stackName:projectName:adoptedResourceType:adoptedResourceName";
        final var alias = Alias.withUrn(urn);
        final var aliasProto = AliasSerializer.serializeAliases(List.of(Output.of(alias))).join().get(0);
        assertThat(aliasProto.getUrn()).isEqualTo(urn);
        assertThat(aliasProto.getSpec().getType()).isEqualTo("");
        assertThat(aliasProto.getSpec().getName()).isEqualTo("");
        assertThat(aliasProto.getSpec().getStack()).isEqualTo("");
        assertThat(aliasProto.getSpec().getProject()).isEqualTo("");
        assertThat(aliasProto.getSpec().getParentUrn()).isEqualTo("");
        assertThat(aliasProto.getSpec().getNoParent()).isEqualTo(false);
    }

    @Test
    void testSerializeNoParentAlias() {
        final var alias = Alias.noParent();
        final var aliasProto = AliasSerializer.serializeAliases(List.of(Output.of(alias))).join().get(0);
        assertThat(aliasProto.getUrn()).isEqualTo("");
        assertThat(aliasProto.getSpec().getType()).isEqualTo("");
        assertThat(aliasProto.getSpec().getName()).isEqualTo("");
        assertThat(aliasProto.getSpec().getStack()).isEqualTo("");
        assertThat(aliasProto.getSpec().getProject()).isEqualTo("");
        assertThat(aliasProto.getSpec().getParentUrn()).isEqualTo("");
        assertThat(aliasProto.getSpec().getNoParent()).isEqualTo(true);
    }

    @Test
    void testSerializeParentUrnAlias()  {
        final var parentUrn = "urn:pulumi:stack::project::pulumi:pulumi:Stack$test:AliasSerializerTest:MyTestParentResource::testParent";
        final var alias = Alias.builder().parentUrn(Output.of(parentUrn)).build();
        final var aliasProto = AliasSerializer.serializeAliases(List.of(Output.of(alias))).join().get(0);
        assertThat(aliasProto.getUrn()).isEqualTo("");
        assertThat(aliasProto.getSpec().getType()).isEqualTo("");
        assertThat(aliasProto.getSpec().getName()).isEqualTo("");
        assertThat(aliasProto.getSpec().getStack()).isEqualTo("");
        assertThat(aliasProto.getSpec().getProject()).isEqualTo("");
        assertThat(aliasProto.getSpec().getParentUrn()).isEqualTo(parentUrn);
        assertThat(aliasProto.getSpec().getNoParent()).isEqualTo(false);
    }

    @Test
    void testSerializeCustomAlias() {
        final var alias = Alias.builder()
            .type("myType")
            .name("myName")
            .stack("myStack")
            .project("myProject")
            .build();
        final var aliasProto = AliasSerializer.serializeAliases(List.of(Output.of(alias))).join().get(0);
        assertThat(aliasProto.getUrn()).isEqualTo("");
        assertThat(aliasProto.getSpec().getType()).isEqualTo("myType");
        assertThat(aliasProto.getSpec().getName()).isEqualTo("myName");
        assertThat(aliasProto.getSpec().getStack()).isEqualTo("myStack");
        assertThat(aliasProto.getSpec().getProject()).isEqualTo("myProject");
        assertThat(aliasProto.getSpec().getParentUrn()).isEqualTo("");
        assertThat(aliasProto.getSpec().getNoParent()).isEqualTo(false);
    }

    @Test
    void testSerializeParentAlias() {
        final var test = PulumiTestInternal.builder()
            .options(TestOptions.builder().preview(false).build())
            .mocks(new AliasSerializerTestMocks(false))
            .build();
        test.runTest(ctx ->
        {
            final var parent = new MyTestParentResource("testParent", null, null);
            final var alias = Alias.builder().parent(parent).build();
            final var aliasProto = AliasSerializer.serializeAliases(List.of(Output.of(alias))).join().get(0);
            assertThat(aliasProto.getUrn()).isEqualTo("");
            assertThat(aliasProto.getSpec().getType()).isEqualTo("");
            assertThat(aliasProto.getSpec().getName()).isEqualTo("");
            assertThat(aliasProto.getSpec().getStack()).isEqualTo("");
            assertThat(aliasProto.getSpec().getProject()).isEqualTo("");
            assertThat(aliasProto.getSpec().getParentUrn())
                .isEqualTo("urn:pulumi:stack::project::pulumi:pulumi:Stack$test:AliasSerializerTest:MyTestParentResource::testParent");
            assertThat(aliasProto.getSpec().getNoParent()).isEqualTo(false);
        }).throwOnError();
    }

    @ResourceType(type = "test:AliasSerializerTest:MyTestParentResource")
    private static final class MyTestParentResource extends CustomResource {
        public MyTestParentResource(String name, @Nullable MyTestParentResourceArgs args,
                                    @Nullable CustomResourceOptions options) {
            super("test:AliasSerializerTest:MyTestParentResource", name,
                  args == null ? new MyTestParentResourceArgs() : args, options);
        }
    }

    private static final class MyTestParentResourceArgs extends ResourceArgs {
        // Empty
    }

    static class AliasSerializerTestMocks implements Mocks {
        private final boolean isPreview;

        AliasSerializerTestMocks(boolean isPreview) {
            this.isPreview = isPreview;
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(CallArgs args) {
            throw new IllegalStateException(String.format("Unknown function %s", args.token));
        }

        @Override
        public CompletableFuture<Mocks.ResourceResult> newResourceAsync(ResourceArgs args) {
            Objects.requireNonNull(args.type);
            if ("test:AliasSerializerTest:MyTestParentResource".equals(args.type)) {
                return CompletableFuture.completedFuture(
                        Mocks.ResourceResult.of(Optional.ofNullable(this.isPreview ? null : "id"), ImmutableMap.<String, Object>of())
                );
            }
            throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
        }
    }


    @AfterEach
    void cleanup() {
        PulumiTestInternal.cleanup();
    }
}
