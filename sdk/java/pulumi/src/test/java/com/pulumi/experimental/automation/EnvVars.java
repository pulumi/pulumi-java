// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to inject environment variables into a test method.
 * See {@link LocalBackendExtension} for an example of how to use this annotation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnvVars {
}
