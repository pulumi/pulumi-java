package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a mark {@link com.pulumi.resources.Resource} output properties.
 * <p>
 * Use this annotation in your Pulumi programs to mark outputs of {@link com.pulumi.resources.ComponentResource}.
 * <br>
 * Requirements:
 * <ul>
 *     <li>annotate a field of type @see {@link com.pulumi.core.Output}</li>
 *     <li>the type {@code T} of the @see {@link com.pulumi.core.Output} needs to be given explicitly using {@link #tree()} and {@link #refs()} parameters</li>
 *     <li>if the type {@code T} is also generic, the tree shape of its generic parameter types must be given using {@link #tree()} parameter otherwise it can be omitted</li>
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
     * @return the generic type parameter tree shape of the annotated @see {@link com.pulumi.core.Output}
     */
    String tree() default "";

    /**
     * @return the generic type parameters of the @see {@link #tree()}
     * If not set, the assumption is that this is not a generic type.
     */
    Class<?>[] refs() default {};
}