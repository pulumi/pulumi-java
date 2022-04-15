package com.pulumi.core.annotations;

import com.pulumi.resources.Stack;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a mark @see {@link com.pulumi.resources.Resource} output properties.
 * <p/>
 * Use this annotation in your Pulumi programs to mark outputs of @see {@link com.pulumi.resources.ComponentResource}
 * and @see {@link Stack} resources.<br/>
 * Requirements:
 * <ul>
 *     <li>annotate a field of type @see {@link com.pulumi.core.Output}</li>
 *     <li>the type {@code T} of the @see {@link com.pulumi.core.Output} needs to be given explicitly using {@code type} parameter</li>
 *     <li>if the type {@code T} is also generic, the list of its generic parameter types mus be given using {@code parameters} parameter</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Export {

    /**
     * @return the exported output name of the annotated @see {@link com.pulumi.core.Output}
     * If not set, the name of the annotated field will be used.
     */
    String name() default "";

    /**
     * @return the generic type parameter of the annotated @see {@link com.pulumi.core.Output}
     */
    Class<?> type();

    /**
     * @return the generic type parameters of the @see {@link #type()}
     * If not set, the assumption is that the @see {@link #type()} is not a generic type.
     */
    Class<?>[] parameters() default {};
}