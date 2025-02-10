package io.pulumi.kubernetes;

import io.pulumi.Log;
import io.pulumi.core.Output;
import io.pulumi.core.internal.Internal;
import io.pulumi.deployment.internal.EngineLogger;
import io.pulumi.kubernetes.ProviderArgs;
import io.pulumi.serialization.internal.Serializer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import io.pulumi.deployment.*;
import io.pulumi.deployment.internal.DeploymentTests;
import io.pulumi.deployment.internal.CurrentDeployment;
import io.pulumi.core.Tuples.Tuple2;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.Map;


class JsonAttrTests {
    @Test
    void verifyKubeConfigNotDoubleEncoded() {
        var deployment = buildDeployment();
        var r = CurrentDeployment.withCurrentDeployment(deployment, () -> {
                var log = new Log(mock(EngineLogger.class));
                var providerArgs = ProviderArgs.builder().kubeconfig(Output.of("kc")).build();
                var map = Internal.from(providerArgs).toOptionalMapAsync(log).join();
                var v = map.get("kubeconfig").get();
                return new Serializer(log).serializeAsync("", v, true).join();
        });
        assertThat(r).isEqualTo("kc");
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
