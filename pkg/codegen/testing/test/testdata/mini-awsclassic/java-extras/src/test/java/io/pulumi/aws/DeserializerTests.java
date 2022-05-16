package com.pulumi.aws;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.pulumi.Log;
import com.pulumi.aws.ec2_getAmi.outputs.GetAmiResult;
import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.OutputData;
import com.pulumi.serialization.internal.Converter;
import com.pulumi.serialization.internal.Deserializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeserializerTests {

    @Test
    void testToleratingProviderNotReturningRequiredProps() {
        var json = "{}";
        TypeShape<GetAmiResult> typeShape = TypeShape.builder(GetAmiResult.class).build();
        var log = Log.ignore();
        var deserializer = new Deserializer(log);
        var converter = new Converter(log, deserializer);
        OutputData<GetAmiResult> responseOutput = converter.convertValue(
                "testContext",
                parseJsonValue(json),
                typeShape,
                ImmutableSet.of());
        GetAmiResult response = responseOutput.getValueOptional().get();
        assertThat(response.kernelId()).isNull();
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
