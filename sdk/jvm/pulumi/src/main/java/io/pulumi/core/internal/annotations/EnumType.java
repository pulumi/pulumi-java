package io.pulumi.core.internal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark enum types.
 * <p>
 * Requirements for an enum to be (de)serialized are as follows.
 * It must:
 * <ul>
 * <li>Be an enum type decorated with {@code EnumType}</li>
 * <li>Have a constructor that takes a single parameter of the underlying type. The constructor can be private.</li>
 * <li>Have a explicit conversion method annotated with @EnumType.Converter that converts the enum type to the underlying type.</li>
 * <li>Implementing toString isn't required, but is recommended and is what our codegen does.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnumType {

    /**
     * Annotation used to mark an explicit converter method that converts the enum type to the underlying type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Converter {
        // Empty
    }
}
