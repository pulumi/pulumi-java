package com.pulumi.provider.internal.schema;


import java.util.Map;
import java.util.Set;

public class ComplexTypeSpec extends ObjectTypeSpec {
    private ComplexTypeSpec(
        String type,
        Map<String, PropertySpec> properties,
        Set<String> required
    ) {
        super(type, properties, required);
    }

    public static ComplexTypeSpec of(String type) {
        return new ComplexTypeSpec(type, null, null);
    }

    public static ComplexTypeSpec ofObject(
        Map<String, PropertySpec> properties,
        Set<String> required
    ) {
        return new ComplexTypeSpec("object", properties, required);
    }
} 