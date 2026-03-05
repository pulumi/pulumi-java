package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the Pulumi resource type and version for a resource class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResourceType {

    /**
     * @return the Resource type
     */
    String type();

    /**
     * @return the Resource version
     */
    String version() default "";
}
