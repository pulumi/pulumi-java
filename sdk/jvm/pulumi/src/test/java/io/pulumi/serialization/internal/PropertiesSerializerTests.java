package io.pulumi.serialization.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.pulumi.Log;
import io.pulumi.core.Input;
import io.pulumi.core.internal.Internal;
import io.pulumi.core.internal.annotations.InputImport;
import io.pulumi.core.internal.annotations.OutputCustomType;
import io.pulumi.deployment.internal.EngineLogger;
import io.pulumi.resources.ResourceArgs;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesSerializerTests {
    @Test
    void verifyJsonAttribute() {
        // InputImport can be marked with json=true.
        // This is currently only used on arguments to provider resources.
        // When serializing json=true properties to protobuf Struct, instead of sending their native
        // Struct representation, we send a protobuf String value with a JSON-formatted Struct representation
        // inside. In the tests below this will look like a double-JSON encoded string since we write asserts
        // against a JSON rendering of the proto struct.
        //
        // For a practical example of where this applies, see Provider resource in pulumi-kubernetes.
        assertThat(showStruct(new ExampleResourceArgs().setB(Input.of(true))))
                .isEqualTo("{\"b\":true}");
        assertThat(showStruct(new ExampleResourceArgs().setBJson(Input.of(true))))
                .isEqualTo("{\"bJson\":\"true\"}");
        assertThat(showStruct(new ExampleResourceArgs().setStr(Input.of("x"))))
                .isEqualTo("{\"str\":\"x\"}");
        assertThat(showStruct(new ExampleResourceArgs().setStrJson(Input.of("x"))))
                .isEqualTo("{\"strJson\":\"\\\"x\\\"\"}");
        assertThat(showStruct(new ExampleResourceArgs().setHelper(Input.of(new HelperArgs(Input.of(1))))))
                .isEqualTo("{\"helper\":{\"intProp\":1.0}}"); // 1 should work also, not 1.0 - Int in the source
        assertThat(showStruct(new ExampleResourceArgs().setHelperJson(Input.of(new HelperArgs(Input.of(1))))))
                .isEqualTo("{\"helperJson\":\"{\\\"intProp\\\":1.0}\"}"); // 1 should work also, not 1.0 - Int in the source
    }

    private static String showStruct(ResourceArgs resourceArgs) {
        var log = new Log(EngineLogger.ignore());
        var args = Internal.from(resourceArgs).toOptionalMapAsync(log).join();
        var s = new PropertiesSerializer(log);
        var label = "LABEL";
        var keepResources = true;
        var struct = s.serializeAllPropertiesAsync(label, args, keepResources).join();
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

    class ExampleResourceArgs extends ResourceArgs {
        @InputImport(name = "str")
        private @Nullable
        Input<String> str;

        @InputImport(name = "strJson", json = true)
        private @Nullable
        Input<String> strJson;

        @InputImport(name = "b")
        private @Nullable
        Input<Boolean> b;

        @InputImport(name = "bJson", json = true)
        private @Nullable
        Input<Boolean> bJson;

        @InputImport(name = "helper")
        private @Nullable
        Input<HelperArgs> helper;

        @InputImport(name = "helperJson", json = true)
        private @Nullable
        Input<HelperArgs> helperJson;

        public ExampleResourceArgs() {
            str = null;
            strJson = null;
            b = null;
            bJson = null;
            helper = null;
            helperJson = null;
        }

        public ExampleResourceArgs setHelperJson(@Nullable Input<HelperArgs> helperJson) {
            this.helperJson = helperJson;
            return this;
        }

        public ExampleResourceArgs setHelper(@Nullable Input<HelperArgs> helper) {
            this.helper = helper;
            return this;
        }

        public ExampleResourceArgs setBJson(@Nullable Input<Boolean> bJson) {
            this.bJson = bJson;
            return this;
        }

        public ExampleResourceArgs setB(@Nullable Input<Boolean> b) {
            this.b = b;
            return this;
        }

        public ExampleResourceArgs setStrJson(@Nullable Input<String> strJson) {
            this.strJson = strJson;
            return this;
        }

        public ExampleResourceArgs setStr(@Nullable Input<String> str) {
            this.str = str;
            return this;
        }
    }

    public class HelperArgs extends ResourceArgs {

        @InputImport(name = "intProp")
        private final @Nullable
        Input<Integer> intProp;

        @OutputCustomType.Constructor({"inProp"})
        private HelperArgs(@Nullable Input<Integer> intProp) {
            this.intProp = intProp;
        }
    }
}
