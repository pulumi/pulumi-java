package com.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.pulumi.Context;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Constants;
import com.pulumi.deployment.internal.TestOptions;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.Resource;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.test.Mocks;
import com.pulumi.test.internal.PulumiTestInternal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.pulumi.serialization.internal.ConverterTests.deserializeFromValue;
import static com.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static com.pulumi.test.PulumiTest.extractValue;
import static com.pulumi.test.internal.PulumiTestInternal.extractOutputData;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceRefPropertyTest {

    private static final Class<ImmutableMap<String, String>> ValuesClass =
            TypeShape.<ImmutableMap<String, String>>builder(ImmutableMap.class)
                    .addParameter(String.class)
                    .addParameter(String.class)
                    .build()
                    .getType();

    @AfterEach
    public void cleanup() {
        PulumiTestInternal.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeCustomResource(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();

        var result = test.runTestAsync(ResourceRefPropertyTest::myStack).join()
                .throwOnError();
        var res = result.resources().stream()
                .filter(r -> r instanceof MyCustomResource)
                .map(r -> (MyCustomResource) r)
                .findFirst()
                .orElse(null);
        assertThat(res).isNotNull();

        var urn = extractValue(res.getUrn());
        var id = extractOutputData(res.getId()).getValueOrDefault("");

        var v = serializeToValueAsync(res).join();

        assertThat(v).isEqualTo(createCustomResourceReference(urn, id));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeCustomResourceDownlevel(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();

        var result = test.runTestAsync(ResourceRefPropertyTest::myStack).join()
                .throwOnError();
        var res = result.resources().stream()
                .filter(r -> r instanceof MyCustomResource)
                .map(r -> (MyCustomResource) r)
                .findFirst()
                .orElse(null);
        assertThat(res).isNotNull();

        var id = serializeToValueAsync(res.getId()).join();
        var v = serializeToValueAsync(res, false).join();

        assertThat(v).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializeCustomResource(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();
        var result = test.runTestAsync(ResourceRefPropertyTest::deserializeCustomResourceStack).join();
        var values = extractValue(result.output("values", ValuesClass));
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
        assertThat(values.get("expectedId")).isEqualTo(values.get("actualId"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializeMissingCustomResource(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();
        var result = test.runTestAsync(ResourceRefPropertyTest::deserializeMissingCustomResourceStack).join();
        var values = extractValue(result.output("values", ValuesClass));
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeComponentResource(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();

        var result = test.runTestAsync(ResourceRefPropertyTest::myStack).join()
                .throwOnError();
        var res = result.resources().stream()
                .filter(r -> r instanceof MyComponentResource)
                .map(r -> (MyComponentResource) r)
                .findFirst()
                .orElse(null);
        assertThat(res).isNotNull();

        var urn = extractValue(res.getUrn());
        var v = serializeToValueAsync(res).join();

        assertThat(v).isEqualTo(createComponentResourceReference(urn));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeComponentResourceDownlevel(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();

        var result = test.runTestAsync(ResourceRefPropertyTest::myStack).join()
                .throwOnError();
        var res = result.resources().stream()
                .filter(r -> r instanceof MyComponentResource)
                .map(r -> (MyComponentResource) r)
                .findFirst()
                .orElse(null);
        assertThat(res).isNotNull();

        var urn = serializeToValueAsync(res.getUrn()).join();
        var v = serializeToValueAsync(res, false).join();

        assertThat(v).isEqualTo(urn);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializeComponentResource(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();

        var result = test.runTestAsync(ResourceRefPropertyTest::deserializeComponentResourceStack).join();
        var values = extractValue(result.output("values", ValuesClass));
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializeMissingComponentResource(boolean isPreview) {
        var test = PulumiTestInternal.builder()
                .options(TestOptions.builder().preview(isPreview).build())
                .mocks(new MyMocks(isPreview))
                .build();
        var result = test.runTestAsync(ResourceRefPropertyTest::deserializeMissingComponentResourceStack).join();
        var values = extractValue(result.output("values", ValuesClass));
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
    }

    private final static class MyArgs extends ResourceArgs {
        // Empty
    }

    @ResourceType(type = "test:index:resource")
    private static class MyCustomResource extends CustomResource {
        public MyCustomResource(String name, @Nullable MyArgs args, @Nullable CustomResourceOptions options) {
            super("test:index:resource", name, args == null ? new MyArgs() : args, options);
        }
    }

    @ResourceType(type = "test:index:component")
    private static class MyComponentResource extends ComponentResource {
        public MyComponentResource(String name, @Nullable MyArgs args, @Nullable ComponentResourceOptions options) {
            super("test:index:component", name, args == null ? new MyArgs() : args, options);
        }
    }

    private static class MissingCustomResource extends CustomResource {
        public MissingCustomResource(String name, @Nullable MyArgs args, @Nullable CustomResourceOptions options) {
            super("test:missing:resource", name, args == null ? new MyArgs() : args, options);
        }
    }

    private static class MissingComponentResource extends ComponentResource {
        public MissingComponentResource(String name, @Nullable MyArgs args, @Nullable ComponentResourceOptions options) {
            super("test:missing:component", name, args == null ? new MyArgs() : args, options);
        }
    }

    public static void myStack(Context context) {
        new MyCustomResource("test", null, null);
        new MyComponentResource("test", null, null);
    }

    public static void deserializeCustomResourceStack(Context ctx) {
        var res = new MyCustomResource("test", null, null);
        var urn = extractOutputData(res.getUrn()).getValueOrDefault("");
        var id = extractOutputData(res.getId()).getValueOrDefault("");
        var v = deserializeFromValue(
                createCustomResourceReference(urn, ""),
                MyCustomResource.class
        );
        var values = Output.of(ImmutableMap.of(
                "expectedUrn", urn,
                "expectedId", id,
                "actualUrn", extractOutputData(v.getUrn()).getValueOrDefault(""),
                "actualId", extractOutputData(v.getId()).getValueOrDefault("")));
        ctx.export("values", values);
    }

    public static void deserializeMissingCustomResourceStack(Context ctx) {
        var res = new MissingCustomResource("test", null, null);
        var urn = extractValue(res.getUrn());
        var v = deserializeFromValue(
                createCustomResourceReference(urn, ""),
                Resource.class
        );
        var values = Output.of(ImmutableMap.of(
                "expectedUrn", urn,
                "actualUrn", extractValue(v.getUrn())
        ));
        ctx.export("values", values);
    }

    public static void deserializeComponentResourceStack(Context ctx) {
        var res = new MyComponentResource("test", null, null);
        var urn = extractValue(res.getUrn());
        var v = deserializeFromValue(
                createComponentResourceReference(urn),
                MyComponentResource.class
        );
        var values = Output.of(ImmutableMap.of(
                "expectedUrn", urn,
                "actualUrn", extractValue(v.getUrn())
        ));
        ctx.export("values", values);
    }

    public static void deserializeMissingComponentResourceStack(Context ctx) {
        var res = new MissingComponentResource("test", null, null);

        var urn = extractValue(res.getUrn());

        var v = deserializeFromValue(
                createComponentResourceReference(urn),
                Resource.class
        );

        var values = Output.of(ImmutableMap.of(
                "expectedUrn", urn,
                "actualUrn", extractValue(v.getUrn())
        ));

        ctx.export("values", values);
    }

    static class MyMocks implements Mocks {
        private final boolean isPreview;

        MyMocks(boolean isPreview) {
            this.isPreview = isPreview;
        }

        @Override
        public CompletableFuture<ResourceResult> newResourceAsync(ResourceArgs args) {
            Objects.requireNonNull(args.type);
            switch (args.type) {
                case "test:index:resource":
                case "test:missing:resource":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.ofNullable(this.isPreview ? null : "id"), ImmutableMap.<String, Object>of())
                    );
                case "test:index:component":
                case "test:missing:component":
                    return CompletableFuture.completedFuture(
                            ResourceResult.of(Optional.empty(), ImmutableMap.<String, Object>of())
                    );
                default:
                    throw new IllegalArgumentException(String.format("Unknown resource '%s'", args.type));
            }
        }
    }

    private static Value createCustomResourceReference(String urn, String id) {
        return Value.newBuilder()
                .setStructValue(Struct.newBuilder()
                        .putFields(Constants.SpecialSigKey, Value.newBuilder().setStringValue(Constants.SpecialResourceSig).build())
                        .putFields("urn", Value.newBuilder().setStringValue(urn).build())
                        .putFields("id", Value.newBuilder().setStringValue(id).build())
                        .build())
                .build();
    }

    private static Value createComponentResourceReference(String urn) {
        return Value.newBuilder()
                .setStructValue(Struct.newBuilder()
                        .putFields(Constants.SpecialSigKey, Value.newBuilder().setStringValue(Constants.SpecialResourceSig).build())
                        .putFields("urn", Value.newBuilder().setStringValue(urn).build())
                        .build())
                .build();
    }

}