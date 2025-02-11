package com.pulumi.provider.internal.properties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import com.pulumi.asset.Asset;
import com.pulumi.asset.Archive;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.core.internal.Internal;
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

    /**
     * Converts a component resource object into a map of property values asynchronously.
     * This method examines the fields of the component resource marked with the {@link Export} annotation
     * and serializes their values into {@link PropertyValue} objects.
     *
     * @param component The component resource object to convert
     * @return A map where keys are property names and values are serialized {@link PropertyValue} objects
     * @throws PropertySerializationException if there is an error accessing field values
     */
    public static CompletableFuture<Map<String, PropertyValue>> stateFromComponentResourceAsync(Object component) {
        Class<?> componentType = component.getClass();
        
        // Get all fields including inherited ones
        Set<Field> fields = new HashSet<>();
        Class<?> currentClass = componentType;
        while (currentClass != null) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        // Process each field with @Export annotation and collect futures
        List<CompletableFuture<Map.Entry<String, PropertyValue>>> futures = fields.stream()
            .filter(field -> field.isAnnotationPresent(Export.class))
            .<CompletableFuture<Map.Entry<String, PropertyValue>>>map(field -> {
                field.setAccessible(true);
                Export attr = field.getAnnotation(Export.class);
                String propertyName = !attr.name().isEmpty() ? attr.name() : field.getName();

                try {
                    Object value = field.get(component);
                    if (value == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return serialize(value, field.getName())
                            .thenApply(serialized -> Map.entry(propertyName, serialized));
                } catch (IllegalAccessException e) {
                    throw new PropertySerializationException(
                        "Failed to get field value",
                        new String[]{field.getName()},
                        field.getType(),
                        e
                    );
                }
            })
            .filter(future -> future != null)
            .collect(Collectors.toList());

        // Wait for all futures to complete and combine results
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(entry -> entry != null)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> a,
                    HashMap::new
                )));
    }

    /**
     * Converts a component resource object into a map of property values.
     * This method examines the fields of the component resource marked with the {@link Export} annotation
     * and serializes their values into {@link PropertyValue} objects.
     *
     * @param component The component resource object to convert
     * @return A map where keys are property names and values are serialized {@link PropertyValue} objects
     * @throws PropertySerializationException if there is an error accessing field values
     */
    public static Map<String, PropertyValue> stateFromComponentResource(Object component) {
        return stateFromComponentResourceAsync(component).join();
    }

    private static CompletableFuture<PropertyValue> serialize(Object value, String... path) {
        if (value == null) {
            return CompletableFuture.completedFuture(PropertyValue.NULL);
        }

        if (value instanceof Output<?>) {
            return serializeOutput((Output<?>) value, path);
        }
        return CompletableFuture.completedFuture(serializeValue(value, path));
    }

    private static CompletableFuture<PropertyValue> serializeOutput(Output<?> output, String... path) {
        return Internal.of(output).getDataAsync().thenCompose(data -> {
            if (!data.isKnown()) {
                return CompletableFuture.completedFuture(PropertyValue.COMPUTED);
            }

            return serialize(data.getValueNullable()).thenApply(element -> {
                var dependencies = data.getResources().stream()
                    .map(resource -> Internal.of(resource.urn()))
                    .map(OutputInternal::getDataAsync)
                    .map(urnFuture -> urnFuture
                        .thenApply(OutputData::getValueNullable)
                        .join())
                    .collect(Collectors.toSet());

                PropertyValue outputValue;
                if (dependencies.isEmpty()) {
                    outputValue = element != null ? element : PropertyValue.COMPUTED;
                } else {
                    outputValue = PropertyValue.of(
                        new PropertyValue.OutputReference(element, dependencies));
                }

                return data.isSecret() ? PropertyValue.ofSecret(outputValue) : outputValue;
            });
        });
    }

    private static PropertyValue serializeValue(Object value, String... path) {
        if (value == null) {
            return PropertyValue.NULL;
        }

        // Handle primitive types and their wrappers
        if (value instanceof String) {
            return PropertyValue.of((String) value);
        }

        if (value instanceof Integer) {
            return PropertyValue.of(((Integer) value).doubleValue());
        }

        if (value instanceof Double || value instanceof Float || value instanceof Long) {
            return PropertyValue.of(((Number) value).doubleValue());
        }

        if (value instanceof Boolean) {
            return PropertyValue.of((Boolean) value);
        }

        // Handle Pulumi-specific types
        if (value instanceof Asset) {
            return PropertyValue.of((Asset) value);
        }

        if (value instanceof Archive) {
            return PropertyValue.of((Archive) value);
        }

        if (value instanceof PropertyValue) {
            return (PropertyValue) value;
        }

        // Handle collections
        if (value instanceof Collection<?>) {
            var array = ((Collection<?>) value).stream()
                .map(v -> serializeValue(v, path))
                .collect(Collectors.toList());
            return PropertyValue.of(array);
        }

        if (value instanceof Map<?, ?>) {
            Map<String, PropertyValue> object = ((Map<?, ?>) value).entrySet().stream()
                .collect(Collectors.toMap(
                    e -> String.valueOf(e.getKey()),
                    e -> serializeValue(e.getValue(), path)
                ));
            return PropertyValue.of(object);
        }

        if (value.getClass().isEnum()) {
            return PropertyValue.of(((Enum<?>) value).ordinal());
        }

        // Handle objects with @Export annotations only
        if (Arrays.stream(value.getClass().getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(Export.class))) {
            Map<String, PropertyValue> object = new HashMap<>();
            for (Field field : value.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Export.class)) {
                    field.setAccessible(true);
                    try {
                        Object fieldValue = field.get(value);
                        if (fieldValue != null) {
                            String propertyName = propertyName(field, value.getClass());
                            // Add the current field name to the path for nested serialization
                            String[] fieldPath = Arrays.copyOf(path, path.length + 1);
                            fieldPath[fieldPath.length - 1] = propertyName;
                            object.put(propertyName, serializeValue(fieldValue, fieldPath));
                        }
                    } catch (IllegalAccessException e) {
                        String[] fieldPath = Arrays.copyOf(path, path.length + 1);
                        fieldPath[fieldPath.length - 1] = field.getName();
                        throw new PropertySerializationException(
                            "Failed to serialize field",
                            fieldPath,
                            field.getType(),
                            e
                        );
                    }
                }
            }
            return PropertyValue.of(object);
        }

        throw new PropertySerializationException(
            String.format("Unsupported type for serialization"),
            path,
            value.getClass(),
            null
        );
    }
}
