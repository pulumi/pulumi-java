package io.pulumi.serialization.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Value;
import io.pulumi.core.Archive;
import io.pulumi.core.Archive.InvalidArchive;
import io.pulumi.core.Asset.InvalidAsset;
import io.pulumi.core.AssetOrArchive;
import io.pulumi.core.Either;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.Maps;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.core.internal.annotations.EnumType;
import io.pulumi.core.internal.annotations.OutputCustomType;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

/**
 * Part of deserialization, @see {@link Deserializer}
 */
@ParametersAreNonnullByDefault
public class Converter {

    private Converter() {
        throw new UnsupportedOperationException("static class");
    }

    public static <T> InputOutputData<T> convertValue(String context, Value value, Class<T> targetType) {
        return convertValue(context, value, TypeShape.of(targetType));
    }

    public static <T> InputOutputData<T> convertValue(String context, Value value, TypeShape<T> targetType) {
        return convertValue(context, value, targetType, ImmutableSet.of());
    }

    public static <T> InputOutputData<T> convertValue(
            String context, Value value, TypeShape<T> targetType, ImmutableSet<Resource> resources
    ) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(value);
        Objects.requireNonNull(targetType);
        Objects.requireNonNull(resources);

        checkTargetType(context, targetType);

        var deserializer = new Deserializer();
        var data = deserializer.deserialize(value);
        // Note: nulls can enter the system as the representation of an 'unknown' value,
        //       but the Deserializer will wrap it in an InputOutputData, and we get them as a null here
        @Nullable
        var converted = convertObjectUntyped(context, data.getValueNullable(), targetType);

        // conversion methods check nested types of the value against the given type shape,
        // so the cast below should be safe in normal circumstances
        if (converted != null && !targetType.getType().isAssignableFrom(converted.getClass())) {
            //noinspection ConstantConditions
            throw new IllegalStateException(String.format(
                    "Expected actual type: '%s' to be cast-able to type: '%s'",
                    converted == null ? null : converted.getClass(), targetType.asString()
            ));
        }

        var mergedResources = ImmutableSet.<Resource>builder()
                .addAll(resources)
                .addAll(data.getResources())
                .build();

        //noinspection unchecked
        return InputOutputData.ofNullable(mergedResources, (T) converted, data.isKnown(), data.isSecret());
    }

    @Nullable
    private static Object convertObjectUntyped(String context, @Nullable Object value, TypeShape<?> targetType) {
        try {
            return tryConvertObjectInner(context, value, targetType);
        } catch (UnsupportedOperationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnsupportedOperationException(String.format(
                    "Convert [%s]: Error converting '%s' to '%s'. %s",
                    context,
                    value == null ? "null" : value.getClass().getTypeName(),
                    targetType,
                    ex.getMessage()
            ), ex);
        }
    }

    @Nullable
    private static Object tryConvertObjectInner(
            String context, @Nullable Object value, TypeShape<?> targetType
    ) {
        var targetIsOptional = Optional.class.isAssignableFrom(targetType.getType());

        // Note: null's  can enter the system as the representation of an 'unknown' value.
        // Before calling 'convert' we will have already lifted the 'isKnown' bit out, but we
        // will be passing null around as a value.
        if (value == null) {
            if (targetIsOptional) {
                // A null value coerces to a Optional.empty.
                return tryEnsureType(
                        String.format("%s %s", context, targetType.getTypeName()),
                        Optional.empty(),
                        targetType
                );
            }

            // We're null and we're NOT converting to an Optional
            // We check for primitives and primitives wrappers here
            if (boolean.class.isAssignableFrom(targetType.getType())
                    || Boolean.class.isAssignableFrom(targetType.getType())) {
                return false;
            }
            if (String.class.isAssignableFrom(targetType.getType())) {
                return "";
            }
            if (double.class.isAssignableFrom(targetType.getType())
                    || Double.class.isAssignableFrom(targetType.getType())) {
                return 0.0;
            }
            if (int.class.isAssignableFrom(targetType.getType())
                    || Integer.class.isAssignableFrom(targetType.getType())) {
                return 1;
            }
            if (JsonElement.class.isAssignableFrom(targetType.getType())) {
                return JsonNull.INSTANCE;
            }

            // for all other types, can just return the null value right back out as a legal
            // reference type value.
            return null;
        }

        // We're Optional
        var valueIsOptional = Optional.class.isAssignableFrom(value.getClass());
        if (valueIsOptional) {
            // We're Optional and we're converting to Optional<T>, just map the value
            if (targetIsOptional) {
                var valueType = targetType.getParameter(0)
                        .orElseThrow(() -> new IllegalArgumentException("Expected the parameter type of the Optional, got none"));
                return ((Optional<?>) value).map(v ->
                        tryConvertObjectInner(
                                String.format("%s %s", context, targetType.getTypeName()),
                                v, // we are guaranteed present at this point
                                valueType
                        ));
            }

            // We're Optional and we're NOT converting to Optional<T>, so unwrap
            //noinspection unchecked
            var unwrapped = ((Optional<Object>) value).orElse(null);
            return tryConvertObjectInner(
                    String.format("%s %s", context, targetType.getTypeName()),
                    unwrapped,
                    targetType
            );
        }

        // We're NOT an Optional but we're converting to Optional<T>, so wrap

        if (targetIsOptional) {
            // wrap in Optional since the type shape requested it
            var wrapped = Optional.of(value);
            return tryConvertObjectInner(
                    String.format("%s %s", context, targetType.getTypeName()),
                    wrapped,
                    targetType
            );
        }

        // We're NOT an Optional and we're NOT converting to Optional<T>, just continue

        if (String.class.isAssignableFrom(targetType.getType())) {
            return tryEnsureType(context, value, targetType);
        }

        if (boolean.class.isAssignableFrom(targetType.getType())
                || Boolean.class.isAssignableFrom(targetType.getType())) {
            return tryEnsureType(context, value, targetType);
        }

        if (double.class.isAssignableFrom(targetType.getType())
                || Double.class.isAssignableFrom(targetType.getType())) {
            return tryEnsureType(context, value, targetType);
        }

        if (int.class.isAssignableFrom(targetType.getType())
                || Integer.class.isAssignableFrom(targetType.getType())) {
            return tryEnsureType(context, value, TypeShape.of(Double.class)).intValue();
        }

        if (Object.class.equals(targetType.getType())) {
            return value;
        }

        if (Archive.class.isAssignableFrom(targetType.getType())) {
            try {
                return tryEnsureType(context, value, targetType);
            } catch (UnsupportedOperationException ex) {
                return tryEnsureType(context, new InvalidArchive(), targetType);
            }
        }

        if (AssetOrArchive.class.isAssignableFrom(targetType.getType())) {
            try {
                return tryEnsureType(context, value, targetType);
            } catch (UnsupportedOperationException ex) {
                return tryEnsureType(context, new InvalidAsset(), targetType);
            }
        }

        if (JsonElement.class.isAssignableFrom(targetType.getType())) {
            return tryConvertJsonElement(context, value);
        }

        if (Resource.class.isAssignableFrom(targetType.getType())) {
            return tryEnsureType(context, value, targetType);
        }

        if (targetType.getType().isEnum()) {
            var converter = targetType.getAnnotatedMethod(EnumType.Converter.class);

            // search enum constants by value
            var constants = ImmutableList.copyOf(targetType.getType().getEnumConstants());
            return constants.stream()
                    .filter(constant -> {
                        try {
                            return Objects.equals(value, converter.invoke(constant));
                        } catch (IllegalAccessException | InvocationTargetException ex) {
                            throw new IllegalStateException(String.format("Unexpected exception: %s", ex.getMessage()), ex);
                        }
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Expected value that match any of enum '%s' constants: [%s], got: '%s'",
                            targetType.getType().getTypeName(),
                            constants.stream()
                                    .map(Object::toString)
                                    .collect(joining(", ")),
                            value
                    )));
        }

        if (Either.class.isAssignableFrom(targetType.getType())) {
            return tryConvertOneOf(context, value, targetType);
        }

        if (List.class.isAssignableFrom(targetType.getType())) {
            return tryConvertList(context, value, targetType);
        }

        if (Map.class.isAssignableFrom(targetType.getType())) {
            return tryConvertMap(context, value, targetType);
        }

        var propertyTypeAnnotation = targetType.getAnnotation(OutputCustomType.class);
        if (propertyTypeAnnotation.isPresent()) {
            var constructor = targetType.getAnnotatedConstructor(OutputCustomType.Constructor.class);
            var constructorAnnotation = Optional.ofNullable(
                    constructor.getAnnotation(OutputCustomType.Constructor.class)
            ).orElseThrow(() -> new IllegalStateException("Expected a constructor annotation.")); // validated before

            //noinspection unchecked
            var argumentsMap = (Map<String, Object>) tryEnsureType(context, value, TypeShape.of(Map.class));
            var constructorParameters = constructor.getParameters();
            var arguments = new Object[constructorParameters.length];

            // Validate that the constructor is annotated properly before doing anything else
            if (constructorParameters.length != constructorAnnotation.value().length) {
                throw new IllegalArgumentException(String.format(
                        "Expected type '%s' (annotated with '%s') to provide a constructor annotated with '%s', " +
                                "and the number of constructor parameters matching the annotated parameter names. " +
                                "Constructor '%s' has '%d' parameters, and '%d' names(s) in the annotation.",
                        targetType.getTypeName(),
                        OutputCustomType.class.getTypeName(),
                        OutputCustomType.Constructor.class.getTypeName(),
                        constructor,
                        constructorParameters.length,
                        constructorAnnotation.value().length
                ));
            }

            // Validate that we can decode the argument we've received
            var expectedParameterNames = Set.of(constructorAnnotation.value());
            for (var argumentName : argumentsMap.keySet()) {
                if (!expectedParameterNames.contains(argumentName)) {
                    System.out.printf("can't deserialize: '%s'\n", argumentName);
                    throw new IllegalStateException(String.format(
                            "Expected type '%s' (annotated with '%s') to provide a constructor annotated with '%s', " +
                                    "and the parameter names in the annotation matching the parameters being deserialized. " +
                                    "Constructor '%s' expects parameter names of: '%s', " +
                                    "but can't deserialize: '%s'",
                            targetType.getTypeName(),
                            OutputCustomType.class.getTypeName(),
                            OutputCustomType.Constructor.class.getTypeName(),
                            constructor,
                            String.join(",", expectedParameterNames),
                            argumentName
                    ));
                }
            }
            for (int i = 0, n = constructorParameters.length; i < n; i++) {
                var parameter = constructorParameters[i];
                var parameterName = constructorAnnotation.value()[i]; // we cannot use parameter.getName(), because it will be just e.g. 'arg0'

                // Note: tryGetValue may not find a value here.
                // That can happen for things like unknown values.
                // That's ok. We'll set the argument as null.
                var argValue = Maps.tryGetValue(argumentsMap, parameterName);
                if (argValue.isPresent()) {
                    arguments[i] = tryConvertObjectInner(
                            String.format("%s(%s)", targetType.getTypeName(), parameterName),
                            argValue,
                            TypeShape.extract(parameter)
                    );
                } else if (parameter.isAnnotationPresent(Nullable.class)) {
                    arguments[i] = null;
                } else {
                    throw new IllegalStateException(String.format(
                            "Expected type '%s' (annotated with '%s') to provide a constructor annotated with '%s', " +
                                    "and the parameter names in the annotation matching the parameters being deserialized. " +
                                    "Constructor '%s' parameter named '%s' (nr %d starting from 0) lacks @%s annotation, " +
                                    "so the value is required, but there is no value to deserialize.",
                            targetType.getTypeName(),
                            OutputCustomType.class.getTypeName(),
                            OutputCustomType.Constructor.class.getTypeName(),
                            constructor,
                            parameterName,
                            i,
                            Nullable.class.getTypeName()
                    ));
                }
            }

            try {
                return constructor.newInstance(arguments);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(String.format("Unexpected exception: %s", e.getMessage()), e);
            }
        }

        if (targetType.getType().isAssignableFrom(Object.class)) {
            return value; // target is not interested in type anymore
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Unexpected target type '%s' when deserializing '%s'", targetType.getTypeName(), context
            ));
        }
    }

    private static JsonElement tryConvertJsonElement(String context, Object value) {
        var gson = new Gson();
        StringWriter stringWriter = new StringWriter();
        try {
            JsonWriter jsonWriter = gson.newJsonWriter(stringWriter);
            tryWriteJson(context, jsonWriter, value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return gson.fromJson(stringWriter.toString(), JsonElement.class);
    }

    private static void tryWriteJson(String context, JsonWriter jsonWriter, @Nullable Object value) throws IOException {
        if (value == null) {
            jsonWriter.nullValue();
            return;
        }
        if (Optional.class.isAssignableFrom(value.getClass())) {
            //noinspection unchecked,rawtypes,rawtypes
            tryWriteJson(context, jsonWriter, ((Optional) value).orElse(null));
            return;
        }
        if (String.class.isAssignableFrom(value.getClass())) {
            jsonWriter.value((String) value);
            return;
        }
        if (double.class.isAssignableFrom(value.getClass()) || Double.class.isAssignableFrom(value.getClass())) {
            jsonWriter.value((Double) value);
            return;
        }
        if (boolean.class.isAssignableFrom(value.getClass()) || Boolean.class.isAssignableFrom(value.getClass())) {
            jsonWriter.value((Boolean) value);
            return;
        }
        if (List.class.isAssignableFrom(value.getClass())) {
            jsonWriter.beginArray();
            //noinspection rawtypes
            for (var e : (List) value) {
                tryWriteJson(context, jsonWriter, e);
            }
            jsonWriter.endArray();
            return;
        }
        if (Map.class.isAssignableFrom(value.getClass())) {
            jsonWriter.beginObject();
            //noinspection unchecked,rawtypes,rawtypes
            for (var e : (Set<Map.Entry>) ((Map) value).entrySet()) {
                jsonWriter.name(tryEnsureType(context, e.getKey(), TypeShape.of(String.class)));
                tryWriteJson(context, jsonWriter, e.getValue());
            }
            jsonWriter.endObject();
            return;
        }
        throw new UnsupportedOperationException(String.format(
                "%s; Unexpected type '%s' when converting to JsonElement",
                context, value.getClass().getTypeName()
        ));
    }

    private static <T> T tryEnsureType(String context, Object value, TypeShape<T> targetType) {
        if (targetType.getType().isInstance(value)
                || (boolean.class.isAssignableFrom(targetType.getType()) && value instanceof Boolean)
                || (double.class.isAssignableFrom(targetType.getType()) && value instanceof Double)
                || (int.class.isAssignableFrom(targetType.getType()) && value instanceof Integer)
        ) {
            //noinspection unchecked
            return (T) value;
        } else {
            throw new UnsupportedOperationException(String.format(
                    "%s; Expected '%s' but got '%s' while deserializing.",
                    context, targetType.getTypeName(), value.getClass().getTypeName()
            ));
        }
    }

    private static Either<Object, Object> tryConvertOneOf(String context, Object value, TypeShape<?> targetType) {
        var leftType = targetType.getParameter(0)
                .orElseThrow(() -> new IllegalStateException("Expected a left parameter type for the Either, got none"));
        var rightType = targetType.getParameter(1)
                .orElseThrow(() -> new IllegalStateException("Expected a left parameter type for the Either, got none"));

        try {
            return Either.ofLeft(
                    tryConvertObjectInner(
                            String.format("%s.left", context),
                            value,
                            leftType
                    )
            );
        } catch (Exception leftException) {
            try {
                return Either.ofRight(
                        tryConvertObjectInner(
                                String.format("%s.right", context),
                                value,
                                rightType
                        )
                );
            } catch (Exception rightException) {
                throw new IllegalArgumentException(String.format(
                        "%s; Can't convert OneOf to Either, got exceptions for both left and right, left: '%s', right: '%s'; Showing stack trace only for the right.",
                        context, leftException.getMessage(), rightException.getMessage()
                ), rightException);
            }
        }
    }

    private static ImmutableList<Object> tryConvertList(String context, Object value, TypeShape<?> targetType) {
        if (!List.class.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "%s; Expected List but got '%s' while deserializing", context, value.getClass().getTypeName()
            ));
        }

        var builder = ImmutableList.builder();
        var elementType = targetType.getParameter(0)
                .orElseThrow(() -> new IllegalArgumentException("Expected a parameter type for the List, got none"));
        //noinspection unchecked
        var objects = (List<Object>) value;
        for (int i = 0, objectsSize = objects.size(); i < objectsSize; i++) {
            builder.add(tryConvertObjectInner(
                    String.format("%s[%d]", targetType.getTypeName(), i),
                    objects.get(i),
                    elementType
            ));
        }
        return builder.build();
    }

    private static ImmutableMap<String, Object> tryConvertMap(String context, Object value, TypeShape<?> targetType) {
        if (!Map.class.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "%s; Expected Map but got '%s' while deserializing", context, value.getClass().getTypeName()
            ));
        }

        var builder = ImmutableMap.<String, Object>builder();
        var valueType = targetType.getParameter(1)
                .orElseThrow(() -> new IllegalArgumentException("Expected a key parameter type for the Map, got none"));

        //noinspection unchecked
        var objects = (Map<String, Object>) value;
        for (var entry : objects.entrySet()) {
            builder.put(entry.getKey(), tryConvertObjectInner(
                    String.format("%s[%s]", targetType.getTypeName(), entry.getKey()),
                    entry.getValue(),
                    valueType
            ));
        }
        return builder.build();
    }

    // TODO

    public static void checkTargetType(String context, TypeShape<?> targetType) {
        checkTargetType(context, targetType, new HashSet<>());
    }

    // pre-check for performance reasons
    public static void checkTargetType(String context, TypeShape<?> targetType, HashSet<Class<?>> seenTypes) {

        // types can be recursive.  So only dive into a type if it's the first time we're seeing it.
        if (!seenTypes.add(targetType.getType())) {
            return;
        }

        // we've reached a primitive or "basic" type - stop condition
        if (boolean.class.isAssignableFrom(targetType.getType()) ||
                Boolean.class.isAssignableFrom(targetType.getType()) ||
                int.class.isAssignableFrom(targetType.getType()) ||
                Integer.class.isAssignableFrom(targetType.getType()) ||
                double.class.isAssignableFrom(targetType.getType()) ||
                Double.class.isAssignableFrom(targetType.getType()) ||
                String.class.isAssignableFrom(targetType.getType()) ||
                AssetOrArchive.class.isAssignableFrom(targetType.getType()) ||
                JsonElement.class.isAssignableFrom(targetType.getType()) ||
                Object.class.equals(targetType.getType())
        ) {
            return;
        }

        if (Resource.class.isAssignableFrom(targetType.getType())) {
            return;
        }

        if (targetType.getType().isEnum()) {
            targetType.getAnnotation(EnumType.class).orElseThrow(
                    () -> new IllegalArgumentException(String.format(
                            "%s; Expected enum type '%s' to be annotated with @%s, not found.",
                            context, targetType.getTypeName(), EnumType.class.getSimpleName()
                    )));

            Function<Class<?>, Boolean> isAllowedGenericArgumentType = type ->
                    String.class.isAssignableFrom(type)
                            || double.class.isAssignableFrom(type)
                            || Double.class.isAssignableFrom(type);

            var converter = targetType.getAnnotatedMethod(EnumType.Converter.class);

            if (converter.getParameterCount() != 0) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected enum type to have a converter that takes zero parameters, got wrong number of parameters: '%s'",
                        context, converter
                ));
            }

            if (!isAllowedGenericArgumentType.apply(converter.getReturnType())) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected enum type to have a converter to String or Double, got: '%s'",
                        context, converter
                ));
            }

            return;
        }

        // Can't do reliable checks here because of erasure
        if (Optional.class.isAssignableFrom(targetType.getType())) {
            if (targetType.getParameterCount() != 1) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected exactly one parameter type in Optional target type '%s', got: %d",
                        context, targetType, targetType.getParameterCount()
                ));
            }
            return;
        }

        if (Either.class.isAssignableFrom(targetType.getType())) {
            if (targetType.getParameterCount() != 2) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected exactly two parameter types in Either target type '%s', got: %d",
                        context, targetType, targetType.getParameterCount()
                ));
            }
            return;
        }

        if (List.class.isAssignableFrom(targetType.getType())) {
            if (targetType.getParameterCount() != 1) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected exactly one parameter type in List target type '%s', got: %d",
                        context, targetType, targetType.getParameterCount()
                ));
            }

            // List value is the 1st out of 2 places that `Object` could appear as a legal value.
            // This type is what is generated for things like YAML decode invocation response
            // in the Kubernetes provider. The elements of the list would typically be immutable maps.
            return;
        }

        if (Map.class.isAssignableFrom(targetType.getType())) {
            if (targetType.getParameterCount() != 2) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected exactly two parameter types in Map target type '%s', got: %d",
                        context, targetType, targetType.getParameterCount()
                ));
            }

            var keyType = targetType.getParameter(0)
                    .orElseThrow(() -> new IllegalArgumentException("Expected a key parameter type in a Map type shape"));

            if (!String.class.isAssignableFrom(keyType.getType())) {
                throw new IllegalArgumentException(String.format(
                        "%s; Expected a key parameter type in a Map type shape to be String, got: '%s'",
                        context, keyType.getTypeName())
                );
            }
            // A Map value is the 2nd out of 2 places that `Object` could appear as a legal value.
            // This type is what is generated for things like azure/aws tags. It's an untyped
            // map in our original schema.
            return;
        }

        var propertyTypeAnnotation = targetType.getAnnotation(OutputCustomType.class);
        if (propertyTypeAnnotation.isPresent()) {
            var constructor = targetType.getAnnotatedConstructor(OutputCustomType.Constructor.class);

            Parameter[] parameters = constructor.getParameters();
            for (Parameter parameter : parameters) {
                checkTargetType(
                        String.format("%s(%s)", targetType.getTypeName(), parameter.getName()),
                        TypeShape.extract(parameter), // check nested target type
                        seenTypes
                );
            }

            return;
        }

        throw new UnsupportedOperationException(String.format(
                "%s; Invalid type '%s' while deserializing. Allowed types are: " +
                        "String, Boolean, Integer, Double, List<> and Map<String, Object> or " +
                        "a class explicitly marked with the @%s.",
                context, targetType.getTypeName(), OutputCustomType.class.getSimpleName()
        ));
    }
}
