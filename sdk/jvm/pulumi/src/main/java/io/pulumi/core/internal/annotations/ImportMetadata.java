package io.pulumi.core.internal.annotations;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Output;
import io.pulumi.core.TypeShape;
import io.pulumi.core.annotations.Import;
import io.pulumi.core.internal.Reflection;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public final class ImportMetadata<F, T, O extends Output<T>> extends ImportExportMetadata<Import, F> {

    private final Import annotation;
    private final TypeShape<O> finalShape;

    private ImportMetadata(Field field, Import annotation, Class<F> fieldType, TypeShape<O> finalShape) {
        super(field, fieldType);
        this.annotation = requireNonNull(annotation);
        this.finalShape = requireNonNull(finalShape);
    }

    @Override
    public Import getAnnotation() {
        return this.annotation;
    }

    @Override
    protected String getAnnotationName() {
        return annotation.name();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", getName())
                .add("annotation", this.getAnnotation())
                .add("fieldType", fieldType)
                .add("finalShape", finalShape)
                .toString();
    }

    public Optional<O> getFieldOutput(Object extractedObject) {
        return getFieldObject(extractedObject)
                .map(value -> {
                    if (Output.class.isAssignableFrom(value.getClass())) {
                        //noinspection unchecked
                        return (O) value;
                    }
                    //noinspection unchecked
                    return (O) Output.of(value);
                });
    }

    private Optional<Object> getFieldObject(Object extractedObject) {
        try {
            var value = this.field.get(extractedObject);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't get the value of a field '%s' annotated with '%s', error: %s",
                    this.field.getName(), this.getAnnotation().annotationType(), e.getMessage()
            ), e);
        }
    }

    /*
     * F (capture#1) - the field type, will be Object or Output
     * T (capture#2) - the final shape value type, will be F or Object
     * O (Output<F || T>) - the final shape type, will be 'Output<F>' or 'F extends Output<T>'
     */
    public static ImmutableMap<String, ImportMetadata<?, ?, ? extends Output<?>>> of(Class<?> extractionType) {
        return Reflection.allFields(extractionType).stream()
                .filter(f -> f.isAnnotationPresent(Import.class))
                .peek(f -> f.setAccessible(true))
                .map(f -> {
                    final var fieldType = f.getType();
                    final var import_ = f.getAnnotation(Import.class);
                    if (fieldType.isAssignableFrom(Output.class)) {
                        // O = F extends Output<Object>
                        //noinspection unchecked
                        return unwrappedMetadata(f, import_, (Class<? extends Output<Object>>) fieldType);
                    } else {
                        // O = Output<F>
                        return wrappedMetadata(f, import_, fieldType);
                    }
                })
                .collect(toImmutableMap(
                        ImportExportMetadata::getName,
                        Function.identity()
                ));
    }

    /**
     * Field is an Output, it does not need to be wrapped in an Output
     */
    private static <F extends Output<Object>> ImportMetadata<F, Object, F> unwrappedMetadata(
            Field field,
            Import import_,
            Class<F> fieldType
    ) {
        if (!fieldType.isAssignableFrom(Output.class)) {
            throw new IllegalStateException(String.format(
                    "Expected 'fieldType' to have type: '%s', got: '%s'. This is a bug.",
                    Output.class.getTypeName(), fieldType.getTypeName()
            ));
        }
        var finalShape = TypeShape.of(fieldType);
        return of(field, import_, fieldType, finalShape);
    }

    /**
     * Field is not an Output, it needs to be wrapped in an Output
     */
    private static <F> ImportMetadata<F, F, Output<F>> wrappedMetadata(
            Field field,
            Import import_,
            Class<F> fieldType
    ) {
        if (fieldType.isAssignableFrom(Output.class)) {
            throw new IllegalStateException(String.format(
                    "Expected 'fieldType' to not have type: '%s', got: '%s'. This is a bug.",
                    Output.class.getTypeName(), fieldType.getTypeName()
            ));
        }
        var finalShape = TypeShape.<Output<F>>builder(Output.class)
                .addParameter(fieldType)
                .build();
        return of(field, import_, fieldType, finalShape);
    }

    private static <F, T, O extends Output<T>> ImportMetadata<F, T, O> of(
            Field field,
            Import inputAnnotation,
            Class<F> fieldType,
            TypeShape<O> finalShape
    ) {
        return new ImportMetadata<>(field, inputAnnotation, fieldType, finalShape);
    }
}
