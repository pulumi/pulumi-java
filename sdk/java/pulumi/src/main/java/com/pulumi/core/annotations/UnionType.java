package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark an interface that stands for a
 * schema union.
 * <p>
 * The interface carries one {@link UnionType.Case} annotation per union member. When the Pulumi
 * runtime deserializes a value into the interface, it selects the one member whose wire shape
 * admits the value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnionType {

    /**
     * Annotation used by a Pulumi Cloud Provider Package to declare one member of a union
     * interface marked with {@link UnionType}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(Case.List.class)
    @interface Case {
        /**
         * @return the class the runtime produces for this member. Either a class that implements
         * the annotated interface, or a case class that wraps a value of the wire type.
         */
        Class<?> type();

        /**
         * @return the class references of the wire type, in the same encoding as
         * {@link Export#refs()}. Empty when the wire type is {@link #type()} itself.
         */
        Class<?>[] refs() default {};

        /**
         * @return the generic tree shape of the wire type, in the same encoding as
         * {@link Export#tree()}
         */
        String tree() default "";

        /**
         * Container for repeated {@link Case} annotations.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        @interface List {
            Case[] value();
        }
    }
}
