package com.pulumi.kubernetes;

import com.pulumi.Log;
import com.pulumi.core.Output;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.Internal;
import com.pulumi.deployment.internal.EngineLogger;
import com.pulumi.kubernetes.ProviderArgs;
import com.pulumi.serialization.internal.Serializer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JsonAttrTests {

    static <T> OutputData<T> waitFor(Output<T> io) {
        return Internal.of(io).getDataAsync().join();
    }

    @Test
    void verifyKubeConfigNotDoubleEncoded() {
        var log = new Log(mock(EngineLogger.class));
        var providerArgs = ProviderArgs.builder().kubeconfig(Output.of("kc")).build();
        var map = Internal.from(providerArgs).toMapAsync(log).join();
        var v = waitFor(map.get("kubeconfig")).getValueNullable();
        var r = new Serializer(log).serializeAsync("", v, true).join();
        assertThat(r).isEqualTo("kc");
    }
}
