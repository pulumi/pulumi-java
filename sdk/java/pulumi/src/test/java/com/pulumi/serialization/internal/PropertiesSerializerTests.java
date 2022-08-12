package com.pulumi.serialization.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.CustomType.Setter;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.EngineLogger;
import com.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesSerializerTests {

    @Test
    void verifyJsonAttribute() {
        // Import can be marked with json=true.
        // This is currently only used on arguments to provider resources.
        // When serializing json=true properties to protobuf Struct, instead of sending their native
        // Struct representation, we send a protobuf String value with a JSON-formatted Struct representation
        // inside. In the tests below this will look like a double-JSON encoded string since we write asserts
        // against a JSON rendering of the proto struct.
        //
        // For a practical example of where this applies, see Provider resource in pulumi-kubernetes.
        assertThat(showStruct(new ExampleResourceArgs().setB(Output.of(true))))
                .isEqualTo("{\"b\":true}");
        assertThat(showStruct(new ExampleResourceArgs().setBJson(Output.of(true))))
                .isEqualTo("{\"bJson\":\"true\"}");
        assertThat(showStruct(new ExampleResourceArgs().setStr(Output.of("x"))))
                .isEqualTo("{\"str\":\"x\"}");
        assertThat(showStruct(new ExampleResourceArgs().setStrJson(Output.of("x"))))
                .isEqualTo("{\"strJson\":\"\\\"x\\\"\"}");
        assertThat(showStruct(new ExampleResourceArgs().setHelper(
                Output.of(HelperArgs.builder().intProp(Output.of(1)).build())
        ))).isEqualTo("{\"helper\":{\"intProp\":1.0}}"); // 1 should work also, not 1.0 - Int in the source
        assertThat(showStruct(new ExampleResourceArgs().setHelperJson(
                Output.of(HelperArgs.builder().intProp(Output.of(1)).build())
        ))).isEqualTo("{\"helperJson\":\"{\\\"intProp\\\":1.0}\"}"); // 1 should work also, not 1.0 - Int in the source
    }

    private static String showStruct(ResourceArgs resourceArgs) {
        var log = new Log(EngineLogger.ignore());
        var args = Internal.from(resourceArgs).toMapAsync(log).join();
        var s = new PropertiesSerializer(log);
        var label = "LABEL";
        var struct = s.serializeAllPropertiesAsync(label, args, true).join();
        return showStruct(struct);
    }

    private static String showStruct(Struct struct) {
        try {
            return JsonFormat.printer()
                    .omittingInsignificantWhitespace()
                    .sortingMapKeys()
                    .print(struct);
        } catch (InvalidProtocolBufferException e) {
            return "ERROR";
        }
    }

    @SuppressWarnings("unused")
    private static class ExampleResourceArgs extends ResourceArgs {

        @Import(name = "str")
        private @Nullable
        Output<String> str;

        @Import(name = "strJson", json = true)
        private @Nullable
        Output<String> strJson;

        @Import(name = "b")
        private @Nullable
        Output<Boolean> b;

        @Import(name = "bJson", json = true)
        private @Nullable
        Output<Boolean> bJson;

        @Import(name = "helper")
        private @Nullable
        Output<HelperArgs> helper;

        @Import(name = "helperJson", json = true)
        private @Nullable
        Output<HelperArgs> helperJson;

        public ExampleResourceArgs() {
            this.str = null;
            this.strJson = null;
            this.b = null;
            this.bJson = null;
            this.helper = null;
            this.helperJson = null;
        }

        public ExampleResourceArgs setHelperJson(@Nullable Output<HelperArgs> helperJson) {
            this.helperJson = helperJson;
            return this;
        }

        public ExampleResourceArgs setHelper(@Nullable Output<HelperArgs> helper) {
            this.helper = helper;
            return this;
        }

        public ExampleResourceArgs setBJson(@Nullable Output<Boolean> bJson) {
            this.bJson = bJson;
            return this;
        }

        public ExampleResourceArgs setB(@Nullable Output<Boolean> b) {
            this.b = b;
            return this;
        }

        public ExampleResourceArgs setStrJson(@Nullable Output<String> strJson) {
            this.strJson = strJson;
            return this;
        }

        public ExampleResourceArgs setStr(@Nullable Output<String> str) {
            this.str = str;
            return this;
        }
    }

    private static class HelperArgs extends ResourceArgs {

        @Import()
        @SuppressWarnings({"FieldCanBeLocal", "unused"})
        private @Nullable Output<Integer> intProp;

        private static Builder builder() {
            return new Builder();
        }

        private static class Builder {
            private final HelperArgs $;

            private Builder() {
                this.$ = new HelperArgs();
            }

            private Builder(HelperArgs defaults) {
                this.$ = defaults;
            }

            @Setter("intProp")
            private Builder intProp(@Nullable Output<Integer> intProp) {
                this.$.intProp = intProp;
                return this;
            }

            private HelperArgs build() {
                return this.$;
            }
        }
    }
}
