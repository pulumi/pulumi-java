package io.pulumi.core.internal.annotations;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.Output;
import io.pulumi.core.internal.Optionals;
import io.pulumi.core.internal.Reflection;
import io.pulumi.core.internal.Reflection.TypeShape;
import io.pulumi.exceptions.RunException;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public final class OutputMetadata<T> extends InputOutputMetadata<OutputExport, Output<T>> {

    private final OutputExport annotation;
    private final TypeShape<T> dataShape;

    private OutputMetadata(Field field, OutputExport annotation, Class<Output<T>> fieldType, TypeShape<T> dataShape) {
        super(field, fieldType);
        this.annotation = requireNonNull(annotation);
        this.dataShape = requireNonNull(dataShape);
    }

    public TypeShape<T> getDataShape() {
        return this.dataShape;
    }

    @Override
    public OutputExport getAnnotation() {
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

    public static ImmutableMap<String, OutputMetadata<?>> of(Class<?> extractionType) {
        return of(extractionType, field -> true);
    }

    public static ImmutableMap<String, OutputMetadata<?>> of(Class<?> extractionType, Predicate<Field> fieldFilter) {
        var fields = Reflection.allFields(extractionType).stream()
                .filter(field1 -> field1.isAnnotationPresent(OutputExport.class))
                .filter(fieldFilter)
                .peek(field1 -> field1.setAccessible(true))
                .collect(toImmutableMap(
                        f -> Optional.ofNullable(f.getAnnotation(OutputExport.class))
                                .flatMap(a -> Optionals.ofBlank(a.name())).orElse(f.getName()),
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
                    "Output(s) '%s' have incorrect type. @%s annotated fields must be instances of Output<>",
                    String.join(", ", wrongFields),
                    OutputExport.class.getSimpleName()
            ));
        }

        return fields.values().stream()
                .map(f -> {
                    @Nullable
                    var export = f.getAnnotation(OutputExport.class);
                    if (export == null) {
                        throw new IllegalArgumentException(String.format(
                                "Expected field '%s' of class '%s' to be annotated with '%s'",
                                f.getName(), f.getDeclaringClass().getTypeName(), OutputExport.class
                        ));
                    }
                    var fieldType = f.getType();
                    var exportType = export.type();
                    var parameters = export.parameters();

                    return of(
                            f, export, fieldType,
                            exportType, parameters
                    );
                })
                .collect(toImmutableMap(
                        InputOutputMetadata::getName,
                        Function.identity()
                ));
    }

    private static <T> OutputMetadata<T> of(
            Field field,
            OutputExport exportAnnotation,
            Class<?> fieldType,
            Class<T> dataType, Class<?>[] dataTypeParameters
    ) {
        //noinspection unchecked
        return new OutputMetadata<>(field, exportAnnotation, (Class<Output<T>>) fieldType,
                TypeShape.builder(dataType).addParameters(dataTypeParameters).build()
        );
    }

    public Output<T> getOrSetFieldValue(Object extractionObject, Output<T> defaultOutput) {
        return getFieldValue(extractionObject).orElseGet(() -> {
            setFieldValue(extractionObject, defaultOutput);
            return defaultOutput;
        });
    }

    public Output<T> getOrSetIncompleteFieldValue(Object extractionObject) {
        // Used to inject OutputCompletionSource, must be completed manually before joining
        return getOrSetFieldValue(extractionObject, Output.of(new CompletableFuture<>()));
    }
}
