package io.pulumi.core.internal.annotations;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.internal.Reflection;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

public final class InputMetadata<F> extends InputOutputMetadata<InputImport, F> {

    private final InputImport annotation;

    private InputMetadata(Field field, InputImport annotation, Class<F> fieldType) {
        super(field, fieldType);
        this.annotation = requireNonNull(annotation);
    }

    @Override
    public InputImport getAnnotation() {
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
                .toString();
    }

    public static ImmutableMap<String, InputMetadata<?>> of(Class<?> extractionType) {
        return of(extractionType, field -> true);
    }

    public static ImmutableMap<String, InputMetadata<?>> of(
            Class<?> extractionType,
            Predicate<Field> fieldFilter
    ) {
        return Reflection.allFields(extractionType).stream()
                .filter(f -> f.isAnnotationPresent(InputImport.class))
                .filter(fieldFilter)
                .map(f -> {
                    f.setAccessible(true);
                    return new InputMetadata<>(
                            f, f.getAnnotation(InputImport.class), f.getType()
                    );
                })
                .collect(toImmutableMap(
                        InputOutputMetadata::getName,
                        Function.identity()
                ));
    }
}
