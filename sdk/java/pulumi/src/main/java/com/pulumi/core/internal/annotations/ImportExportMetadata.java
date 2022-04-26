package com.pulumi.core.internal.annotations;

import com.pulumi.core.internal.Optionals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public abstract class ImportExportMetadata<A extends Annotation, F> {

    protected final Field field;
    protected final Class<F> fieldType;

    @SuppressWarnings("unused")
    private ImportExportMetadata() {
        throw new UnsupportedOperationException("static class");
    }

    protected ImportExportMetadata(Field field, Class<F> fieldType) {
        this.field = requireNonNull(field);
        this.fieldType = requireNonNull(fieldType);
    }

    public abstract A getAnnotation();

    protected abstract String getAnnotationName();

    public String getName() {
        return Optionals.ofBlank(getAnnotationName()).orElse(getFieldName());
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

    public boolean isFieldNull(Object extractedObject) {
        try {
            return this.field.get(extractedObject) == null;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't get the value of a field '%s' annotated with '%s', error: %s",
                    this.field.getName(), this.getAnnotation().annotationType(), e.getMessage()
            ), e);
        }
    }

    public F getFieldValueOrElse(Object extractedObject, Supplier<F> defaultValue) {
        return getFieldValue(extractedObject).orElseGet(defaultValue);
    }

    public <X extends RuntimeException> F getFieldValueOrThrow(Object extractedObject, Supplier<X> exceptionSupplier) {
        return getFieldValue(extractedObject).orElseThrow(exceptionSupplier);
    }

    public Optional<F> getFieldValue(Object extractedObject) {
        try {
            //noinspection unchecked
            var value = (F) this.field.get(extractedObject);
            return Optional.ofNullable(value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't get the value of a field '%s' annotated with '%s', error: %s",
                    this.field.getName(), this.getAnnotation().annotationType(), e.getMessage()
            ), e);
        }
    }

    public void setFieldValue(Object extractedObject, F output) {
        try {
            this.field.set(extractedObject, output);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(String.format(
                    "Can't set the value of a field '%s' annotated with '%s', error: %s",
                    this.field.getName(), this.getAnnotation().annotationType(), e.getMessage()
            ), e);
        }
    }
}
