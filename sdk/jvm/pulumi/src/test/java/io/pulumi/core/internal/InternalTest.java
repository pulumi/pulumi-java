package io.pulumi.core.internal;

import io.pulumi.core.internal.Internal.Field;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTest {

    public static MyClass.Internal of(MyClass my) {
        return Internal.from(my, MyClass.Internal.class);
    }

    @Test
    void testInternalFactory() {
        assertThat(of(new MyClass()).getSecret()).isEqualTo("secret");
    }

    public static class MyClass {
        private final String secret = "secret";

        @SuppressWarnings("unused")
        @Field
        private final Internal internal = new Internal();

        private final class Internal {
            private Internal() { /* Empty */ }

            public String getSecret() {
                return MyClass.this.secret;
            }
        }
    }
}