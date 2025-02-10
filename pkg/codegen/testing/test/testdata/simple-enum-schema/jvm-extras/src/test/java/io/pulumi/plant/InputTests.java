package io.pulumi.plant;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.pulumi.Log;
import io.pulumi.core.Output;
import io.pulumi.core.Either;
import io.pulumi.plant.inputs.*;
import io.pulumi.plant.enums.*;
import io.pulumi.core.Output;
import io.pulumi.core.internal.OutputData;
import io.pulumi.core.internal.Internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.pulumi.deployment.*;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.CurrentDeployment;
import io.pulumi.core.Tuples.Tuple2;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.Map;


class InputTests {

    static <T> OutputData<T> waitFor(Output<T> io) {
        return Internal.of(io).getDataAsync().join();
    }

    @Test
    void testContainerArgs_nullValues() {
        var deployment = buildDeployment();
        CurrentDeployment.withCurrentDeployment(deployment, () -> {
                var args = ContainerArgs.Empty;
                var map = Internal.from(args).toOptionalMapAsync(mock(Log.class)).join();
                assertThat(map).containsKey("brightness");
                assertThat(map).containsKey("color");
                assertThat(map).containsKey("material");
                assertThat(map).containsKey("size");

                assertThat(waitFor((Output) map.get("brightness").get())).isEqualTo(waitFor(Output.empty()));
                assertThat(waitFor((Output) map.get("color").get())).isEqualTo(waitFor(Output.empty()));
                assertThat(waitFor((Output) map.get("material").get())).isEqualTo(waitFor(Output.empty()));
                assertThat(waitFor((Output) map.get("size").get())).isEqualTo(waitFor(Output.empty()));
                return (Void) null;
            });
    }

    @Test
    void testContainerArgs_simpleValues() {
        var deployment = buildDeployment();
        CurrentDeployment.withCurrentDeployment(deployment, () -> {
            var args = ContainerArgs.builder()
                .brightness(ContainerBrightness.ZeroPointOne)
                .color(Output.of(Either.ofLeft(ContainerColor.Red)))
                .material("glass")
                .size(ContainerSize.FourInch)
                .build();

            var map = Internal.from(args).toOptionalMapAsync(mock(Log.class)).join();

            assertThat(map).containsKey("brightness");
            assertThat(map).containsKey("color");
            assertThat(map).containsKey("material");
            assertThat(map).containsKey("size");

            assertThat(waitFor((Output) map.get("brightness").get())).isEqualTo(waitFor(Output.of(ContainerBrightness.ZeroPointOne)));
            assertThat(waitFor((Output) map.get("color").get())).isEqualTo(waitFor(Output.ofLeft(ContainerColor.Red)));
            assertThat(waitFor((Output) map.get("material").get())).isEqualTo(waitFor(Output.of("glass")));
            assertThat(waitFor((Output) map.get("size").get())).isEqualTo(waitFor(Output.of(ContainerSize.FourInch)));

            return (Void) null;
        });
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
