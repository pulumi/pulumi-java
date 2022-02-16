package io.pulumi.core.internal.annotations;

import io.pulumi.core.InputOutput;
import io.pulumi.core.internal.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

public abstract class InputOutputMetadata<A extends Annotation> {

    protected final Field field;

    @SuppressWarnings("unused")
    private InputOutputMetadata() {
        throw new UnsupportedOperationException("static class");
    }

    protected InputOutputMetadata(Field field) {
        this.field = Objects.requireNonNull(field);
    }

    public abstract A getAnnotation();

    protected abstract String getAnnotationName();

    public String getName() {
        return Strings.emptyToOptional(getAnnotationName()).orElse(getFieldName());
    }

    public String getFieldName() {
        return this.field.getName();
    }

    public Class<?> getFieldType() {
        return this.field.getType();
    }

    public String generateFullName(Class<?> subtype) {
        return String.format("%s.%s", subtype.getTypeName(), this.getName());
    }

    public Optional<Object> getFieldValue(Object extractionObject) {
        try {
            return Optional.ofNullable(this.field.get(extractionObject));
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException("Can't get the value of an annotated field, error: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("rawtypes")
    public <IO extends InputOutput> void setFieldValue(Object extractionObject, IO output) {
        try {
            this.field.set(extractionObject, output);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException("Can't set the value of an annotated field. " + e.getMessage(), e);
        }
    }
}
