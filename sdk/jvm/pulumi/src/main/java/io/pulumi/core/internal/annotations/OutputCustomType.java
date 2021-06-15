package io.pulumi.core.internal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark complex types used for a Resource
 * output property.
 * <p/>
 * A complex type must have a single constructor
 * marked with the @see {@link OutputCustomType.Constructor} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OutputCustomType {

    /**
     * Annotation used by a Pulumi Cloud Provider Package to marks the constructor for a complex
     * property type so that it can be instantiated by the Pulumi runtime.
     * <p/>
     * <b>WARNING</b>: nested classes will have a reference to the parent class as the
     * as the first parameter of the constructor (arg0) and this type also needs to be deserializable
     * or the class needs to be made static. This is unlikely scenario, but theoretically possible.
     * The invisible fist argument needs also to be named in the annotation value.
     * <p/>
     * The constructor should contain parameters that map to
     * the resultant @see {@link com.google.protobuf.Struct} returned by the engine.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    @interface Constructor {
        /**
         * We need to know the names of the constructor parameters,
         * and unfortunately Java compiler does not give this information through reflection by default
         * @return names of the constructor parameters in order from left to right
         */
        String[] value();
    }
}
