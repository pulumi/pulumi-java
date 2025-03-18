package com.pulumi.core.annotations;

import com.pulumi.resources.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PolicyPackType {
    /**
     * @return the Policy Pack name
     */
    String name();

    /**
     * @return the optional version of the Policy Pack
     */
    String version() default "1.0.0";
}
