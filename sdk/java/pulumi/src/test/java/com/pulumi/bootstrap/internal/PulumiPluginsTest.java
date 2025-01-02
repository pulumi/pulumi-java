package com.pulumi.bootstrap.internal;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PulumiPluginsTest {

    @Test
    void verifyEqualsAndHashCode() {
        EqualsVerifier.forClass(PulumiPlugin.class).verify();
    }

    @Test
    void testFromClasspath() {
        var packages = PulumiPlugins.fromClasspath(PulumiPluginsTest.class);
        assertThat(packages).hasSize(1).containsEntry(
                // minimal example, it derives both name and version
                "com/pulumi/unittest", new PulumiPlugin(true, "unittest", "1.1.1", null, null)
        );
    }
}