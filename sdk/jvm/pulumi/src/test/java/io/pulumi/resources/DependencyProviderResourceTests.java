package io.pulumi.resources;

import io.pulumi.core.internal.Internal;
import io.pulumi.resources.internal.DependencyProviderResource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyProviderResourceTests {

    @Test
    void testGetPackage() {
        var res = new DependencyProviderResource("urn:pulumi:stack::project::pulumi:providers:aws::default_4_13_0");
        assertThat(Internal.from(res).getPackage()).isEqualTo("aws");
    }
}
