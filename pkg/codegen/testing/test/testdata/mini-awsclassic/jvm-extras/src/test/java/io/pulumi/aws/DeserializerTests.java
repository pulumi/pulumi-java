package io.pulumi.aws;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import io.pulumi.Log;
import io.pulumi.aws.ec2_getAmi.outputs.GetAmiResult;
import io.pulumi.core.TypeShape;
import io.pulumi.core.internal.OutputData;
import io.pulumi.serialization.internal.Converter;
import io.pulumi.serialization.internal.Deserializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeserializerTests {

    @Test
    void testToleratingProviderNotReturningRequiredProps() {
        var json = "{}";
        TypeShape<GetAmiResult> typeShape = TypeShape.builder(GetAmiResult.class).build();
        var deserializer = new Deserializer();
        var converter = new Converter(Log.ignore(), deserializer);
        OutputData<GetAmiResult> responseOutput = converter.convertValue(
                "testContext",
                parseJsonValue(json),
                typeShape,
                ImmutableSet.of());
        GetAmiResult response = responseOutput.getValueOptional().get();
        assertThat(response.getKernelId()).isNull();
    }

    private Value parseJsonValue(String jsonText) {
        var valueBuilder = Value.newBuilder();
        try {
            JsonFormat.parser().merge(jsonText, valueBuilder);
            return valueBuilder.build();
        } catch (InvalidProtocolBufferException e) {
            assertThat(e).isNull();
            return null;
        }
    }
}
