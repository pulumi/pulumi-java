package io.pulumi.core.internal.annotations;

import io.pulumi.core.internal.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public abstract class InputOutputMetadata<A extends Annotation, F> {

    protected final Field field;
    protected final Class<F> fieldType;

    @SuppressWarnings("unused")
    private InputOutputMetadata() {
        throw new UnsupportedOperationException("static class");
    }

    protected InputOutputMetadata(Field field, Class<F> fieldType) {
        this.field = requireNonNull(field);
        this.fieldType = requireNonNull(fieldType);
    }

    public abstract A getAnnotation();

    protected abstract String getAnnotationName();

    public String getName() {
        return Strings.emptyToOptional(getAnnotationName()).orElse(getFieldName());
    }

    public String getFieldName() {
        return this.field.getName();
    }

    public Class<F> getFieldType() {
        //noinspection unchecked
        return (Class<F>) this.field.getType();
    }

    public String generateFullName(Class<?> subtype) {
        return String.format("%s.%s", subtype.getTypeName(), this.getName());
    }

    public Optional<F> getFieldValue(Object extractionObject) {
        try {
            //noinspection unchecked
            return Optional.ofNullable((F) this.field.get(extractionObject));
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't get the value of a field '%s' annotated with '%s', error: %s",
                    this.field.getName(), this.getAnnotation().annotationType(), e.getMessage()
            ), e);
        }
    }

    public void setFieldValue(Object extractionObject, F output) {
        try {
            this.field.set(extractionObject, output);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't set the value of a field '%s' annotated with '%s', error: %s",
                    this.field.getName(), this.getAnnotation().annotationType(), e.getMessage()
            ), e);
        }
    }
}
