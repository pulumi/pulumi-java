package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark a property whose schema pins it to
 * one constant value.
 * <p>
 * Place it on a {@link CustomType.Setter} method or on an {@link Import} field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ConstValue {
    /**
     * @return the constant, rendered as a string. The runtime parses it according to the type of
     * the annotated property.
     */
    String value();
}
