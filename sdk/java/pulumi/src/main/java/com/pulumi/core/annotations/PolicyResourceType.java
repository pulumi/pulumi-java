package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PolicyResourceType
{

    /**
     * @return the Resource type for the policy
     */
    String type();

    /**
     * @return the Resource version
     */
    String version() default "";
}
