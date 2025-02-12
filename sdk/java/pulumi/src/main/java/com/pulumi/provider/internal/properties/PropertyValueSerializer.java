package com.pulumi.provider.internal.properties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.pulumi.asset.Asset;
import com.pulumi.asset.Archive;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.OutputInternal;
import com.pulumi.resources.DependencyResource;
import com.pulumi.resources.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * A utility class for deserializing {@link PropertyValue} objects into Java types.
 * This class handles deserialization of primitive types, collections, maps, and complex objects
 * with support for Pulumi's {@link Output} types and annotations.
 */
public final class PropertyValueSerializer {
    private PropertyValueSerializer() {}

    /**
     * Deserializes a PropertyValue into the specified target type.
     *
     * @param value the PropertyValue to deserialize
     * @param targetType the Class to deserialize into
     * @param <T> the type to deserialize to
     * @return the deserialized object of type T
     * @throws PropertyDeserializationException if the value cannot be deserialized to the target type
     * @throws IllegalArgumentException if value or targetType is null
     */
    public static <T> T deserialize(PropertyValue value, Class<T> targetType) throws PropertyDeserializationException {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        Object deserialized = deserializeValue(value, targetType);
        
        if (targetType.isInstance(deserialized)) {
            return targetType.cast(deserialized);
        }

        throw new PropertyDeserializationException(
            "Could not deserialize value", 
            new String[]{"$"}, 
            targetType, 
            value,
            null
        );
    }

    private static Object deserializeValue(PropertyValue value, java.lang.reflect.Type targetType) {
        String[] rootPath = new String[]{"$"};
        return deserializeValue(value, targetType, rootPath);
    }

    private static Object deserializeValue(PropertyValue value, java.lang.reflect.Type targetType, String[] path) {
        Class<?> rawType = resolveGenericType(targetType);

        if (Output.class.isAssignableFrom(rawType)) {
            return deserializeOutput(value, targetType, path);
        }

        if (value.isNull() && isNullable(rawType)) {
            return null;
        }

        switch (value.getType()) {
            case STRING:
                if (rawType == String.class) {
                    return value.getStringValue();
                }
                break;
            case NUMBER:
                // Get the raw number value first
                Number numberValue = (Number)value.getValue(Number.class);
                
                if (rawType == Integer.class || rawType == int.class) {
                    return numberValue.intValue();
                }
                if (rawType == Double.class || rawType == double.class) {
                    return numberValue.doubleValue();
                }
                if (rawType == Number.class) {
                    return numberValue;
                }
                if (rawType.isEnum()) {
                    return rawType.getEnumConstants()[numberValue.intValue()];
                }
                break;
            case BOOL:
                if (rawType == Boolean.class || rawType == boolean.class) {
                    return value.getBooleanValue();
                }
                break;
            case ARRAY:
                if (Collection.class.isAssignableFrom(rawType)) {
                    return deserializeCollection(value, targetType, path);
                }
                break;
            case ASSET:
                if (rawType == Asset.class) {
                    return value.getAssetValue();
                }
                break;
            case ARCHIVE:
                if (rawType == Archive.class) {
                    return value.getArchiveValue();
                }
                break;
            case OBJECT:
                if (Map.class.isAssignableFrom(rawType)) {
                    return deserializeMap(value, rawType, path);
                }
                return deserializeComplexObject(value.getObjectValue(), rawType, path);
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported type for deserialization: %s to %s, path: %s", 
                        value.getType().name(), rawType.getName(), path));
        }

        throw new IllegalArgumentException(
            String.format("Unsupported type for deserialization: %s to %s, path: %s", 
                value.getType().name(), rawType.getName(), path));
    }

    private static Object deserializeOutput(PropertyValue value, java.lang.reflect.Type targetType, String[] path) {
        Object deserializedValue = null;
        boolean isKnown = true;
        boolean isSecret = false;
        ImmutableSet<Resource> resources = ImmutableSet.of();

        // Get the actual type parameter from Output<T>
        java.lang.reflect.Type elementType = Object.class;
        if (targetType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) targetType;
            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                elementType = typeArgs[0]; // Keep as Type instead of converting to Class
            }
        }

        PropertyValue valueToDeserialize = null;
        switch (value.getType()) {
            case SECRET:
                isSecret = true;
                valueToDeserialize = value.getSecretValue();
                break;
            case OUTPUT:
                var outputRef = value.getOutputValue();
                valueToDeserialize = outputRef.getValue();
                if (valueToDeserialize != null) {
                    resources = ImmutableSet.copyOf(
                        outputRef.getDependencies().stream()
                            .map(DependencyResource::new)
                            .collect(Collectors.toSet())
                    );
                }
                break;
            case COMPUTED:
                isKnown = false;
                break;
            default:
                // For all other types, use the value as-is
                valueToDeserialize = value;
                break;
        }

        if (valueToDeserialize != null && !value.isComputed()) {
            deserializedValue = deserializeValue(valueToDeserialize, elementType, path);
        }

        OutputData<?> outputData = OutputData.ofNullable(resources, deserializedValue, isKnown, isSecret);
        return new OutputInternal<>(outputData);
    }

    private static Class<?> resolveGenericType(java.lang.reflect.Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) type;
            // Just return the raw type, don't try to resolve type parameters
            return (Class<?>) paramType.getRawType();
        }
        if (type instanceof java.lang.reflect.TypeVariable) {
            java.lang.reflect.Type[] bounds = ((java.lang.reflect.TypeVariable<?>) type).getBounds();
            if (bounds.length > 0) {
                return resolveGenericType(bounds[0]);
            }
        }
        return Object.class;
    }

    private static Object deserializeCollection(PropertyValue value, java.lang.reflect.Type targetType, String[] path) {
        var array = value.getArrayValue();
        
        // Get the element type from the collection's type parameter
        java.lang.reflect.Type elementType = Object.class;
        if (targetType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) targetType;
            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                elementType = typeArgs[0];
            }
        }

        Collection<Object> collection = createCollection(resolveGenericType(targetType));

        for (int i = 0; i < array.size(); i++) {
            var elementPath = Arrays.copyOf(path, path.length + 1);
            elementPath[elementPath.length - 1] = String.format("index[%d]", i);
            var element = deserializeValue(array.get(i), elementType, elementPath);
            collection.add(element);
        }

        return collection;
    }

    private static Object deserializeMap(PropertyValue value, java.lang.reflect.Type targetType, String[] path) {
        var objectValue = value.getObjectValue();
        
        java.lang.reflect.Type valueType = Object.class;
        if (targetType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) targetType;
            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 1) {
                valueType = typeArgs[1];
            }
        }

        Map<String, Object> map = createMap(resolveGenericType(targetType));

        for (var entry : objectValue.entrySet()) {
            var elementPath = Arrays.copyOf(path, path.length + 1);
            elementPath[elementPath.length - 1] = entry.getKey();
            var element = deserializeValue(entry.getValue(), valueType, elementPath);
            map.put(entry.getKey(), element);
        }

        return map;
    }

    private static Object deserializeComplexObject(Map<String, PropertyValue> objectValue, Class<?> targetType, String[] path) {
        // Create instance using no-args constructor
        Object instance;
        try {
            Constructor<?> noArgsConstructor = targetType.getDeclaredConstructor();
            noArgsConstructor.setAccessible(true);
            instance = noArgsConstructor.newInstance();
        } catch (Exception e) {
            throw new PropertyDeserializationException(
                "Failed to create instance", 
                path, 
                targetType, 
                null, 
                e
            );
        }

        // Set fields using Import annotations
        for (Field field : targetType.getDeclaredFields()) {
            field.setAccessible(true);
            String propertyName = propertyName(field, targetType);
            
            PropertyValue value = objectValue.get(propertyName);
            if (value == null) {
                Import importAnnotation = field.getAnnotation(Import.class);
                if (importAnnotation != null && importAnnotation.required()) {
                    throw new IllegalArgumentException(
                            String.format("Missing required field %s in type %s",
                                    propertyName, targetType.getName()));
                }
                continue;
            }

            var fieldPath = Arrays.copyOf(path, path.length + 1);
            fieldPath[fieldPath.length - 1] = propertyName;
            
            try {
                // Use the field's generic type instead of just its class
                Object deserializedValue = deserializeValue(value, field.getGenericType(), fieldPath);
                field.set(instance, deserializedValue);
            } catch (IllegalAccessException e) {
                throw new PropertyDeserializationException(
                    "Failed to set field: " + field.getName(), 
                    fieldPath, 
                    field.getType(), 
                    value, 
                    e
                );
            }
        }

        return instance;
    }

    private static Collection<Object> createCollection(Class<?> rawType) {
        if (rawType.isInterface()) {
            return new ArrayList<>();
        }
        try {
            @SuppressWarnings("unchecked")  // Safe because we know this is a Collection type
            Collection<Object> collection = (Collection<Object>) rawType.getDeclaredConstructor().newInstance();
            return collection;
        } catch (Exception e) {
            throw new PropertyDeserializationException(
                "Failed to create collection instance", 
                new String[]{"$"}, 
                rawType, 
                null, 
                e
            );
        }
    }

    private static Map<String, Object> createMap(Class<?> targetType) {
        if (targetType.isInterface()) {
            return new HashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")  // Safe because we know this is a Map type
            Map<String, Object> map = (Map<String, Object>) targetType.getDeclaredConstructor().newInstance();
            return map;
        } catch (Exception e) {
            throw new PropertyDeserializationException(
                "Failed to create map instance", 
                new String[]{"$"}, 
                targetType, 
                null, 
                e
            );
        }
    }

    private static boolean isNullable(Class<?> type) {
        return !type.isPrimitive();
    }

    private static String propertyName(Field field, Class<?> declaringType) {
        String propertyName = field.getName();
        
        Export exportAnnotation = field.getAnnotation(Export.class);
        if (exportAnnotation != null && !exportAnnotation.name().isEmpty()) {
            propertyName = exportAnnotation.name();
        }

        Import importAnnotation = field.getAnnotation(Import.class);
        if (importAnnotation != null && !importAnnotation.name().isEmpty()) {
            propertyName = importAnnotation.name();
        }

        return propertyName;
    }
}
