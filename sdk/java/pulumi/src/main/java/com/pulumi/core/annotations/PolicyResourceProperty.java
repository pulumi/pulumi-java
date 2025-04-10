package com.pulumi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PolicyResourceProperty
{

    /**
     * @return the name of the property for this field
     */
    String name();

    /**
     * @return the name of the flag for this property
     */
    String flag();
}
