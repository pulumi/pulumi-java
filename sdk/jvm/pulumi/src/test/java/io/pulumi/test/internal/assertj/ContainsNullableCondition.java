package io.pulumi.test.internal.assertj;

import org.assertj.core.api.Condition;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;

public class ContainsNullableCondition<ELEMENT> extends Condition<Iterable<? extends ELEMENT>> {
    @Nullable
    private final Iterable<? extends ELEMENT> iterable;

    public ContainsNullableCondition(@Nullable Iterable<? extends ELEMENT> values) {
        super("contains " + values);
        this.iterable = values;
    }

    public static <ELEMENT> ContainsNullableCondition<ELEMENT> containsNullable(@Nullable Iterable<? extends ELEMENT> set) {
        return new ContainsNullableCondition<>(set);
    }

    @Override
    public boolean matches(@Nullable Iterable<? extends ELEMENT> actual) {
        if (iterable == null && actual == null) {
            return true;
        }
        //noinspection ConstantConditions
        if (iterable == null && actual != null) {
            return false;
        }
        //noinspection ConstantConditions
        if (iterable != null && actual == null) {
            return false;
        }
        for (Iterator<? extends ELEMENT> iteratorActual = actual.iterator(), iteratorOther = iterable.iterator(); iteratorOther.hasNext(); ) {
            if (!iteratorActual.hasNext()) {
                return false;
            }
            ELEMENT thiz = iteratorActual.next();
            ELEMENT other = iteratorOther.next();
            if (!Objects.equals(thiz, other)) {
                return false;
            }
        }
        return true;
    }

    ;
}
