package io.pulumi.core.internal;

import com.google.common.collect.ImmutableList;
import io.pulumi.core.internal.Reflection.TypeAwareConstructor;
import org.junit.jupiter.api.Test;

class ReflectionTest {

    static class TestValue<E> extends Reflection.TypeAware<ImmutableList<E>> {
        @TypeAwareConstructor
        private TestValue(ImmutableList<E> value) {
            super(value);
        }
        public TestValue(E value) {
            this(ImmutableList.of(value));
        }
    }

    @Test
    void test() {
        var tester = new TestValue<>("test");
        System.out.println(tester.getTypeShape());
        System.out.println(tester.getTypeShape().getParameters());
        System.out.println(tester.getTypeToken());
        System.out.println(tester.getTypeClass());
    }
}