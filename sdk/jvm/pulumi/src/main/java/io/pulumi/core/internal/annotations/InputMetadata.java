package io.pulumi.core.internal.annotations;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.pulumi.core.internal.Reflection;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

public final class InputMetadata extends InputOutputMetadata<InputImport> {

    private final InputImport annotation;

    private InputMetadata(Field field, InputImport annotation) {
        super(field);
        this.annotation = Objects.requireNonNull(annotation);
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
                .toString();
    }

    public static ImmutableMap<String, InputMetadata> of(Class<?> extractionType) {
        return of(extractionType, field -> true);
    }

    public static ImmutableMap<String, InputMetadata> of(
            Class<?> extractionType,
            Predicate<Field> fieldFilter
    ) {
        return Reflection.allFields(extractionType).stream()
                .filter(field1 -> field1.isAnnotationPresent(InputImport.class))
                .filter(fieldFilter)
                .map(field1 -> {
                    field1.setAccessible(true);
                    return new InputMetadata(
                            field1, field1.getAnnotation(InputImport.class)
                    );
                })
                .collect(toImmutableMap(
                        InputOutputMetadata::getName,
                        Function.identity()
                ));
    }
}
