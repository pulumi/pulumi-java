// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.pulumi.core.internal.annotations.InternalUse;

@InternalUse
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SkipIfFalse {
}
