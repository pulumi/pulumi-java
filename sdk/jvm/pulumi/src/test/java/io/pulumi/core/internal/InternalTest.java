package io.pulumi.core.internal;

import io.pulumi.core.internal.Internal.InternalField;
import io.pulumi.core.internal.InternalTest.MyClass.MyClassInternal;
import io.pulumi.core.internal.InternalTest.MySubClass.MySubClassInternal;
import io.pulumi.core.internal.InternalTest.MySubSubClass.MySubSubClassInternal;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class InternalTest {

    @Test
    void testInternalFactory() {
        assertThat(Internal.from(new MyClass(), MyClassInternal.class).getSecret()).isEqualTo("secret");

        assertThat(Internal.from(new MyClass(), MyClassInternal.class))
                .isExactlyInstanceOf(MyClassInternal.class);

        assertThat(Internal.from(new MySubClass(), MyClassInternal.class))
                .isExactlyInstanceOf(MyClassInternal.class);
        assertThat(Internal.from(new MySubClass(), MySubClassInternal.class))
                .isExactlyInstanceOf(MySubClassInternal.class);

        assertThat(Internal.from(new MySubSubClass(), MyClassInternal.class))
                .isExactlyInstanceOf(MyClassInternal.class);
        assertThat(Internal.from(new MySubSubClass(), MySubClassInternal.class))
                .isExactlyInstanceOf(MySubClassInternal.class);
        assertThat(Internal.from(new MySubSubClass(), MySubSubClassInternal.class))
                .isExactlyInstanceOf(MySubSubClassInternal.class);

        assertThat(Internal.from(new MyInternalLessClass(), MyClassInternal.class))
                .isExactlyInstanceOf(MyClassInternal.class);
        assertThat(Internal.from(new MyInternalLessClass(), MySubClassInternal.class))
                .isExactlyInstanceOf(MySubClassInternal.class);
        assertThat(Internal.from(new MyInternalLessClass(), MySubSubClassInternal.class))
                .isExactlyInstanceOf(MySubSubClassInternal.class);
    }

    public static class MyClass {
        private final String secret = "secret";

        @SuppressWarnings("unused")
        @InternalField
        private final MyClassInternal internal = new MyClassInternal(this);

        static class MyClassInternal {
            private final MyClass myClass;

            public MyClassInternal(MyClass myClass) {
                this.myClass = requireNonNull(myClass);
            }

            public String getSecret() {
                return this.myClass.secret;
            }
        }
    }

    public static class MySubClass extends MyClass {
        @InternalField
        private final MySubClassInternal internal = new MySubClassInternal(this);

        static class MySubClassInternal extends MyClassInternal {
            private MySubClassInternal(MySubClass mySubClass) {
                super(mySubClass);
            }
        }
    }

    public static class MySubSubClass extends MySubClass {
        @InternalField
        private final MySubSubClassInternal internal = new MySubSubClassInternal(this);

        static final class MySubSubClassInternal extends MySubClassInternal {
            private MySubSubClassInternal(MySubSubClass mySubSubClass) {
                super(mySubSubClass);
            }
        }
    }

    public static class MyInternalLessClass extends MySubSubClass {
        // Empty
    }
}