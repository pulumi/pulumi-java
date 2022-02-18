package io.pulumi.kubernetes;

import io.pulumi.Log;
import io.pulumi.core.Input;
import io.pulumi.deployment.internal.EngineLogger;
import io.pulumi.kubernetes.ProviderArgs;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JsonAttrTests {
    @Test
    void verifyKubeConfigNotDoubleEncoded() {
        var log = new Log(mock(EngineLogger.class));
        var providerArgs = ProviderArgs.builder().setKubeconfig(Input.of("kc")).build();
        var map = providerArgs.internalToOptionalMapAsync(log).join();
        assertThat((String)map.get("kubeconfig").get()).isEqualTo("kc");
    }
}
