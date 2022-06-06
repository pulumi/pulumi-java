package com.pulumi.serialization.internal;

import com.google.protobuf.Value;

import java.util.function.Consumer;

public final class ValueVisitor {
    public static void visit(Value value, Consumer<Value> consumer) {
        consumer.accept(value);
        switch (value.getKindCase()) {
            case KIND_NOT_SET:
                throw new IllegalArgumentException("Unexpected value with KIND_NOT_SET");
            case LIST_VALUE:
                for (Value v : value.getListValue().getValuesList()) {
                    visit(v, consumer);
                }
                return;
            case STRUCT_VALUE:
                for (Value v : value.getStructValue().getFieldsMap().values()) {
                    visit(v, consumer);
                }
                return;
        }
    }
}
