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

class JsonAttrTests {
    @Test
    void verifyKubeConfigNotDoubleEncoded() {
        var log = new Log(mock(EngineLogger.class));
        var providerArgs = ProviderArgs.builder().kubeconfig(Output.of("kc")).build();
        var map = Internal.from(providerArgs).toOptionalMapAsync(log).join();
        var v = map.get("kubeconfig").get();
        var r = new Serializer(log).serializeAsync("", v, true).join();
        assertThat(r).isEqualTo("kc");
    }
}
