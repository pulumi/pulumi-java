package io.pulumi.serialization.internal;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.pulumi.core.internal.Maps;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Optional;

import static com.google.protobuf.Value.KindCase.STRING_VALUE;
import static com.google.protobuf.Value.KindCase.STRUCT_VALUE;

@ParametersAreNonnullByDefault
@InternalUse
public class Structs {

    private Structs() {
        throw new UnsupportedOperationException("static class");
    }

    public static Optional<String> tryGetStringValue(Struct struct, String keyName) {
        return tryGetStringValue(struct.getFieldsMap(), keyName);
    }

    public static Optional<String> tryGetStringValue(Map<String, Value> fields, String keyName) {
        return fields.entrySet().stream()
                .filter(entry -> entry.getKey().equals(keyName))
                .filter(entry -> entry.getValue().getKindCase() == STRING_VALUE)
                .map(entry -> entry.getValue().getStringValue())
                .findFirst();
    }

    public static Optional<Struct> tryGetStructValue(Struct struct, String keyName) {
        return tryGetStructValue(struct.getFieldsMap(), keyName);
    }

    public static Optional<Struct> tryGetStructValue(Map<String, Value> fields, String keyName) {
        return fields.entrySet().stream()
                .filter(entry -> entry.getKey().equals(keyName))
                .filter(entry -> entry.getValue().getKindCase() == STRUCT_VALUE)
                .map(entry -> entry.getValue().getStructValue())
                .findFirst();
    }

    public static Optional<Value> tryGetValue(Struct struct, String keyName) {
        return tryGetValue(struct.getFieldsMap(), keyName);
    }

    public static Optional<Value> tryGetValue(Map<String, Value> fields, String keyName) {
        return Maps.tryGetValue(fields, keyName);
    }
}
