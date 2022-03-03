package io.pulumi.azurenative;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import io.pulumi.azurenative.web.outputs.SiteConfigResponse;
import io.pulumi.core.TypeShape;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.serialization.internal.Converter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class Regress164Tests {

    @Test
    void test164() {
        var json = "{\"netFrameworkVersion\": \"4.5\"}";
        TypeShape<SiteConfigResponse> typeShape = TypeShape.builder(SiteConfigResponse.class).build();
        InputOutputData<SiteConfigResponse> responseOutput = Converter.convertValue(
                "testContext",
                parseJsonValue(json),
                typeShape,
                ImmutableSet.of());
        SiteConfigResponse response = responseOutput.getValueOptional().get();
        assertThat(response.getMachineKey()).isNotNull();
        assertThat(response.getNetFrameworkVersion().get()).isEqualTo("4.5");
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
