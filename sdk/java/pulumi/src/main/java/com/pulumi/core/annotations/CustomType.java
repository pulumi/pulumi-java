package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark complex types used for a Resource
 * output property.
 * <p>
 * A complex type must have a single constructor
 * marked with the @see {@link CustomType.Constructor} annotation,
 * or a single builder marked with the @see {@link CustomType.Builder} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomType {

    /**
     * Annotation used by a Pulumi provider to mark the constructor for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     * <p>
     * <b>WARNING:</b> nested classes will have a reference to the parent class
     * as the first parameter of the constructor ({@code arg0})
     * and this type also needs to be deserializable
     * or the class needs to be made static.
     * This is unlikely scenario, but theoretically possible.
     * The invisible fist argument needs also to be named in the annotation value.
     * <p>
     * The constructor must take parameters annotated with {@link Parameter} that map to
     * the resultant @see {@link com.google.protobuf.Struct} returned by the engine.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    @interface Constructor {
        /* Empty */
    }

    /**
     * Annotation used by a Pulumi provider to mark a builder for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     * <p>
     * The setter must take a parameter annotated with {@link Parameter} that map to
     * the resultant @see {@link com.google.protobuf.Struct} returned by the engine.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Builder {
        /* Empty */
    }

    /**
     * Annotation used by a Pulumi provider to mark a setter for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     * <p>
     * The setter must take a parameter annotated with {@link Parameter} that map to
     * the resultant @see {@link com.google.protobuf.Struct} returned by the engine.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Setter {
        /* Empty */
    }

    /**
     * Annotation used by a Pulumi Cloud Provider Package to marks a constructor parameter for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Parameter {
        /**
         * We need to know the name of a constructor parameter,
         * and unfortunately Java compiler does not give this information through reflection (by default)
         * @return name of a constructor parameter
         */
        String value();
    }
}
