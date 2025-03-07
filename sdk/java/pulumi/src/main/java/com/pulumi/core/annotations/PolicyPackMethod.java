package com.pulumi.core.annotations;

import com.pulumi.resources.Resource;
import pulumirpc.AnalyzerOuterClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PolicyPackMethod {
    /**
     * @return the Resource type for this policy
     */
    Class<? extends Resource> value();

    /**
     * @return the Policy name
     */
    String name();

    /**
     * @return the Policy description
     */
    String description();

    /**
     * @return the Policy enforcement level
     */
    AnalyzerOuterClass.EnforcementLevel enforcementLevel();
}
