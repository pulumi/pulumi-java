package io.pulumi.kubernetes;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import io.pulumi.Log;
import io.pulumi.kubernetes.ProviderArgs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JsonAttrTests {
    @Test
    void verifyStuff() {
        var providerArgs = ProviderArgs.builder().setKubeconfig(Input.of("kc")).build();


        assertThat(1).isEqualTo(1);
    }
}
