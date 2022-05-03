package io.pulumi.core;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EitherTest {

    @Test
    void verifyEqualsAndHashCode() {
        EqualsVerifier.forClass(Left.class).suppress(Warning.NULL_FIELDS).verify();
        EqualsVerifier.forClass(Right.class).suppress(Warning.NULL_FIELDS).verify();
    }

    @Test
    void testFlatMap() {
        assertThat(
                Either.valueOf("1").flatMap(s -> Either.valueOf(Integer.parseInt(s))).right()
        ).isEqualTo(1);

        assertThat(
                Either.errorOf(new RuntimeException("test correct")).flatMap(s -> {
                    throw new IllegalStateException("test is broken");
                }).left()
        ).isNotNull()
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("test correct");
    }
}