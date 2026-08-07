package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package to mark an interface that stands for a
 * discriminated union of complex types.
 * <p>
 * The interface must also carry one {@link DiscriminatedUnion.Case} annotation per union member.
 * When deserializing, the Pulumi runtime reads the property named by {@link #value()} from the raw
 * value and instantiates the member whose tag matches, instead of guessing from the value shape.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscriminatedUnion {
    /**
     * @return the name of the wire property whose value selects a union member
     */
    String value();

    /**
     * Annotation used by a Pulumi Cloud Provider Package to declare one member of a discriminated
     * union interface marked with {@link DiscriminatedUnion}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(Case.List.class)
    @interface Case {
        /**
         * @return the discriminator property value that selects this member
         */
        String tag();

        /**
         * @return the concrete type to instantiate, which must implement the annotated interface
         */
        Class<?> type();

        /**
         * Container for repeated {@link Case} annotations. Java requires it, but generated code
         * never names it: repeated {@code @DiscriminatedUnion.Case} annotations are collected here
         * by the compiler.
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.TYPE)
        @interface List {
            Case[] value();
        }
    }
}
