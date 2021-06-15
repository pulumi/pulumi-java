package io.pulumi.core;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TuplesTest {

    @Test
    public void verifyEqualsAndHashCode() {
        EqualsVerifier.forClass(Tuples.Tuple2.class).verify();
        EqualsVerifier.forClass(Tuples.Tuple3.class).verify();
        EqualsVerifier.forClass(Tuples.Tuple4.class).verify();
        EqualsVerifier.forClass(Tuples.Tuple5.class).verify();
        EqualsVerifier.forClass(Tuples.Tuple6.class).verify();
        EqualsVerifier.forClass(Tuples.Tuple7.class).verify();
        EqualsVerifier.forClass(Tuples.Tuple8.class).verify();
    }
}