package com.pulumi.core.internal.annotations;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Reflection;
import com.pulumi.exceptions.RunException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public final class ExportMetadata<T> extends ImportExportMetadata<Export, Output<T>> {

    private final Export annotation;
    private final TypeShape<T> dataShape;

    private ExportMetadata(Field field, Export annotation, Class<Output<T>> fieldType, TypeShape<T> dataShape) {
        super(field, fieldType);
        this.annotation = requireNonNull(annotation);
        this.dataShape = requireNonNull(dataShape);
    }

    public TypeShape<T> getDataShape() {
        return this.dataShape;
    }

    @Override
    public Export getAnnotation() {
        return annotation;
    }

    @Override
    protected String getAnnotationName() {
        return getAnnotation().name();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", getName())
                .add("annotation", annotation)
                .add("fieldType", fieldType)
                .add("exportShape", dataShape)
                .toString();
    }

    public static ImmutableMap<String, ExportMetadata<?>> of(Class<?> extractionType) {
        return of(extractionType, field -> true);
    }

    public static ImmutableMap<String, ExportMetadata<?>> of(Class<?> extractionType, Predicate<Field> fieldFilter) {
        var fields = Reflection.allFields(extractionType).stream()
                .filter(f -> f.isAnnotationPresent(Export.class))
                .filter(fieldFilter)
                .peek(f -> f.setAccessible(true))
                .collect(toImmutableMap(
                        Field::getName,
                        Function.identity()
                ));

        // check if Output annotated fields have the correct type;
        // it would be easier to validate on construction,
        // but we aggregate all errors here for user's convenience
        var wrongFields = fields.entrySet().stream()
                // check if the field has type allowed by the annotation
                .filter(entry -> !Output.class.isAssignableFrom(entry.getValue().getType()))
                .map(Map.Entry::getKey)
                .collect(toImmutableList());

        if (!wrongFields.isEmpty()) {
            throw new RunException(String.format(
                    "Output field(s) '%s' have incorrect type. @%s annotated fields must be instances of Output<>",
                    String.join(", ", wrongFields),
                    Export.class.getSimpleName()
            ));
        }

        return fields.values().stream()
                .map(field -> {
                    var export = Optional.ofNullable(field.getAnnotation(Export.class))
                            .orElseThrow(() -> new IllegalStateException(String.format(
                                    "Expected field '%s' of class '%s' to be annotated with '%s'. This is a bug.",
                                    field.getName(), field.getDeclaringClass().getTypeName(), Export.class.getTypeName()
                            )));

                    var fieldType = field.getType();
                    var exportTypeNew = TypeShape.fromTree(export.refs(), export.tree());
                    return of(field, export, fieldType, exportTypeNew);
                })
                .collect(toImmutableMap(
                        ImportExportMetadata::getName,
                        Function.identity()
                ));
    }

    private static <T> ExportMetadata<T> of(
            Field field,
            Export exportAnnotation,
            Class<?> fieldType,
            TypeShape<T> dataType
    ) {
        //noinspection unchecked
        return new ExportMetadata<>(field, exportAnnotation, (Class<Output<T>>) fieldType, dataType);
    }

    private static <T> ExportMetadata<T> of(
            Field field,
            Export exportAnnotation,
            Class<?> fieldType,
            Class<T> dataType, Class<?>[] dataTypeParameters
    ) {
        //noinspection unchecked
        return new ExportMetadata<>(field, exportAnnotation, (Class<Output<T>>) fieldType,
                TypeShape.builder(dataType).addParameters(dataTypeParameters).build()
        );
    }

    public Output<T> getOrSetFieldValue(Object extractionObject, Output<T> defaultOutput) {
        return getFieldValueOrElse(extractionObject, () -> {
            setFieldValue(extractionObject, defaultOutput);
            return defaultOutput;
        });
    }

    public Output<T> getOrSetIncompleteFieldValue(Object extractionObject) {
        // Used to inject OutputCompletionSource, must be completed manually before joining
        return getOrSetFieldValue(extractionObject, Output.of(new CompletableFuture<>()));
    }
}
