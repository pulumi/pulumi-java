package io.pulumi.core.internal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a mark @see {@link io.pulumi.resources.Resource} output properties.
 * <p/>
 * Use this annotation in your Pulumi programs to mark outputs of @see {@link io.pulumi.resources.ComponentResource}
 * and @see {@link io.pulumi.Stack} resources.<br/>
 * Requirements:
 * <ul>
 *     <li>annotate a field of type @see {@link io.pulumi.core.Output}</li>
 *     <li>the type {@code T} of the @see {@link io.pulumi.core.Output} needs to be given explicitly using {@code type} parameter</li>
 *     <li>if the type {@code T} is also generic, the list of its generiv parameter types mus be given using {@code parameters} parameter</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OutputExport {

    /**
     * @return the exported output name of the annotated @see {@link io.pulumi.core.Output}
     */
    String name() default "";

    /**
     * @return the generic type parameter of the annotated @see {@link io.pulumi.core.Output}
     */
    Class<?> type();

    /**
     * @return the generic type parameters of the @see {@link #type()}
     */
    Class<?>[] parameters() default {};
}