package com.pulumi.core;

import com.pulumi.core.internal.Internal;
import com.pulumi.test.PulumiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrnTest {

    @BeforeAll
    public static void mockSetup() {
        PulumiTest.withDefaults().build();
    }

    @AfterAll
    static void cleanup() {
        PulumiTest.cleanup();
    }

    @Test
    void testCreateUrnInputOutput() {
        var urn = Urn.create(
                Output.of("name"),
                Output.of("type"),
                null,
                null,
                null,
                null
        );

        var value = Internal.of(urn).getValueNullable().join();
        assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
    }

    @Test
    void testCreateUrnString() {
        var urn = Urn.create(
                Output.of("name"),
                Output.of("type"),
                null,
                null,
                null,
                null
        );

        var value = Internal.of(urn).getValueNullable().join();
        assertThat(value).isEqualTo("urn:pulumi:stack::project::type::name");
    }
}
