package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark complex types used for a Resource
 * output property.
 * <p>
 * A complex type must have a single builder marked with the {@link CustomType.Builder} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomType {
    /**
     * Annotation used by a Pulumi provider to mark a builder for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Builder {
        /* Empty */
    }

    /**
     * Annotation used by a Pulumi provider to mark a setter for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Setter {
        /**
         * We need to know the name of a parameter expected by the deserializer,
         * and unfortunately Java compiler does not give this information through reflection (by default)
         *
         * @return name of a parameter (defaults to the setter method name)
         */
        String value() default "";
    }
}