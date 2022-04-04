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
import org.junit.jupiter.api.Test;
import io.pulumi.deployment.*;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.core.Tuples.Tuple2;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class DeserializerTests {

    @Test
    void testToleratingProviderNotReturningRequiredProps() {
        var json = "{}";
        TypeShape<GetAmiResult> typeShape = TypeShape.builder(GetAmiResult.class).build();
        var converter = new Converter(() -> buildDeployment(), Log.ignore());
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

    private static Deployment buildDeployment() {
        var mock = DeploymentTests.DeploymentMockBuilder.builder()
                    .setMocks(new MocksThatAlwaysThrow())
                    .buildSpyInstance();
        return mock.getDeployment();
    }

    public static class MocksThatAlwaysThrow implements Mocks {
        @Override
        public CompletableFuture<Tuple2<Optional<String>, Object>> newResourceAsync(MockResourceArgs args) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                                                                            "MocksThatAlwaysThrow do not support resources"));
        }

        @Override
        public CompletableFuture<Map<String, Object>> callAsync(MockCallArgs args) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                                                                            "MocksThatAlwaysThrow do not support calls"));
        }
    }
}
