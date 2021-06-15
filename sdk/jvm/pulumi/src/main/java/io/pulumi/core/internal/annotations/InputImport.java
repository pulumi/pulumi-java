package io.pulumi.core.internal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by a Pulumi Cloud Provider Package
 * to mark @see {@link io.pulumi.resources.Resource} input fields.
 * <p/>
 * Note: this should just be placed on the field itself, not a setter or getter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InputImport {
    String name() default "";

    boolean required() default false;

    boolean json() default false;
}
