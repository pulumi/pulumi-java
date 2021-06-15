package io.pulumi.serialization.internal;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.pulumi.Stack;
import io.pulumi.core.InputOutputTests;
import io.pulumi.core.Output;
import io.pulumi.core.Tuples;
import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.annotations.OutputExport;
import io.pulumi.core.internal.annotations.ResourceType;
import io.pulumi.deployment.MockCallArgs;
import io.pulumi.deployment.MockMonitor;
import io.pulumi.deployment.MockResourceArgs;
import io.pulumi.deployment.Mocks;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.resources.*;
import io.pulumi.test.internal.TestOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.pulumi.deployment.internal.DeploymentTests.cleanupDeploymentMocks;
import static io.pulumi.deployment.internal.DeploymentTests.printErrorCount;
import static io.pulumi.serialization.internal.ConverterTests.deserializeFromValue;
import static io.pulumi.serialization.internal.ConverterTests.serializeToValueAsync;
import static org.assertj.core.api.Assertions.assertThat;

class ResourceRefPropertyTest {

    private static DeploymentTests.DeploymentMock mock;

    @AfterEach
    public void cleanup() {
        cleanupDeploymentMocks();
    }

    @AfterEach
    public void printInternalErrorCount() {
        printErrorCount(mock.logger);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeCustomResource(boolean isPreview) {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack.class).join();
        var res = resources.stream()
                .filter(r -> r instanceof MyCustomResource)
                .map(r -> (MyCustomResource) r)
                .findFirst()
                .orElse(null);
        assertThat(res).isNotNull();

        var urn = InputOutputTests.waitFor(res.getUrn()).getValueNullable();
        var id = InputOutputTests.waitFor(res.getId()).getValueNullable();

        var v = serializeToValueAsync(res).join();

        assertThat(v).isEqualTo(createCustomResourceReference(urn, id));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeCustomResourceDownlevel(boolean isPreview) {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack.class).join();
        var res = resources.stream()
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
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(DeserializeCustomResourceStack.class).join();
        var stack = resources.stream()
                .filter(r -> r instanceof DeserializeCustomResourceStack)
                .map(r -> (DeserializeCustomResourceStack) r)
                .findFirst()
                .orElse(null);
        assertThat(stack).isNotNull();

        var values = InputOutputTests.waitFor(stack.values).getValueNullable();
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
        assertThat(values.get("expectedId")).isEqualTo(values.get("actualId"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializeMissingCustomResource(boolean isPreview) {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(DeserializeMissingCustomResourceStack.class).join();
        var stack = resources.stream()
                .filter(r -> r instanceof DeserializeMissingCustomResourceStack)
                .map(r -> (DeserializeMissingCustomResourceStack) r)
                .findFirst()
                .orElse(null);
        assertThat(stack).isNotNull();

        var values = InputOutputTests.waitFor(stack.values).getValueNullable();
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeComponentResource(boolean isPreview) {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack.class).join();
        var res = resources.stream()
                .filter(r -> r instanceof MyComponentResource)
                .map(r -> (MyComponentResource) r)
                .findFirst()
                .orElse(null);
        assertThat(res).isNotNull();

        var urn = InputOutputTests.waitFor(res.getUrn()).getValueNullable();
        var v = serializeToValueAsync(res).join();

        assertThat(v).isEqualTo(createComponentResourceReference(urn));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSerializeComponentResourceDownlevel(boolean isPreview) {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(MyStack.class).join();
        var res = resources.stream()
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
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(DeserializeComponentResourceStack.class).join();
        var stack = resources.stream()
                .filter(r -> r instanceof DeserializeComponentResourceStack)
                .map(r -> (DeserializeComponentResourceStack) r)
                .findFirst()
                .orElse(null);
        assertThat(stack).isNotNull();

        var values = InputOutputTests.waitFor(stack.values).getValueNullable();
        assertThat(values).isNotNull();
        assertThat(values.get("expectedUrn")).isEqualTo(values.get("actualUrn"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializeMissingComponentResource(boolean isPreview) {
        mock = DeploymentTests.DeploymentMockBuilder.builder()
                .setOptions(new TestOptions(isPreview))
                .setMonitor(new MockMonitor(new MyMocks(isPreview)))
                .setSpyGlobalInstance();

        var resources = mock.testAsync(DeserializeMissingComponentResourceStack.class).join();
        var stack = resources.stream()
                .filter(r -> r instanceof DeserializeMissingComponentResourceStack)
                .map(r -> (DeserializeMissingComponentResourceStack) r)
                .findFirst()
                .orElse(null);
        assertThat(stack).isNotNull();

        var values = InputOutputTests.waitFor(stack.values).getValueNullable();
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

    public static class MyStack extends Stack {
        public MyStack() {
            new MyCustomResource("test", null, null);
            new MyComponentResource("test", null, null);
        }
    }

    public static class DeserializeCustomResourceStack extends Stack {

        @OutputExport(type = ImmutableMap.class, parameters = {String.class, String.class})
        public final Output<ImmutableMap<String, String>> values;

        public DeserializeCustomResourceStack() {
            var res = new MyCustomResource("test", null, null);

            var urn = InputOutputTests.waitFor(res.getUrn()).getValueNullable();
            var id = InputOutputTests.waitFor(res.getId()).getValueNullable();

            var v = deserializeFromValue(
                    createCustomResourceReference(urn, ""),
                    MyCustomResource.class
            );

            this.values = Output.of(ImmutableMap.of(
                    "expectedUrn", urn,
                    "expectedId", id,
                    "actualUrn", InputOutputTests.waitFor(v.getUrn()).getValueNullable(),
                    "actualId", InputOutputTests.waitFor(v.getId()).getValueNullable()
            ));
        }
    }

    public static class DeserializeMissingCustomResourceStack extends Stack {

        @OutputExport(type = ImmutableMap.class, parameters = {String.class, String.class})
        public final Output<ImmutableMap<String, String>> values;

        public DeserializeMissingCustomResourceStack() {
            var res = new MissingCustomResource("test", null, null);

            var urn = InputOutputTests.waitFor(res.getUrn()).getValueNullable();

            var v = deserializeFromValue(
                    createCustomResourceReference(urn, ""),
                    Resource.class
            );

            this.values = Output.of(ImmutableMap.of(
                    "expectedUrn", urn,
                    "actualUrn", InputOutputTests.waitFor(v.getUrn()).getValueNullable()
            ));
        }
    }

    public static class DeserializeComponentResourceStack extends Stack {

        @OutputExport(type = ImmutableMap.class, parameters = {String.class, String.class})
        public final Output<ImmutableMap<String, String>> values;

        public DeserializeComponentResourceStack() {
            var res = new MyComponentResource("test", null, null);

            var urn = InputOutputTests.waitFor(res.getUrn()).getValueNullable();

            var v = deserializeFromValue(
                    createComponentResourceReference(urn),
                    MyComponentResource.class
            );

            this.values = Output.of(ImmutableMap.of(
                    "expectedUrn", urn,
                    "actualUrn", InputOutputTests.waitFor(v.getUrn()).getValueNullable()
            ));
        }
    }

    public static class DeserializeMissingComponentResourceStack extends Stack {

        @OutputExport(type = ImmutableMap.class, parameters = {String.class, String.class})
        public Output<ImmutableMap<String, String>> values;

        public DeserializeMissingComponentResourceStack() {
            var res = new MissingComponentResource("test", null, null);

            var urn = InputOutputTests.waitFor(res.getUrn()).getValueNullable();

            var v = deserializeFromValue(
                    createComponentResourceReference(urn),
                    Resource.class
            );

            this.values = Output.of(ImmutableMap.of(
                    "expectedUrn", urn,
                    "actualUrn", InputOutputTests.waitFor(v.getUrn()).getValueNullable()
            ));
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
            switch (args.type) {
                case "test:index:resource":
                case "test:missing:resource":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.ofNullable(this.isPreview ? null : "id"), ImmutableMap.<String, Object>of())
                    );
                case "test:index:component":
                case "test:missing:component":
                    return CompletableFuture.completedFuture(
                            Tuples.of(Optional.empty(), ImmutableMap.<String, Object>of())
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