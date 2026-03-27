// Copyright 2025, Pulumi Corporation

package com.pulumi.automation.serialization.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.pulumi.core.internal.annotations.InternalUse;

/**
 * Annotation indicating that a boolean field should be skipped during serialization if its value is {@code false}.
 */
@InternalUse
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SkipIfFalse {
}
