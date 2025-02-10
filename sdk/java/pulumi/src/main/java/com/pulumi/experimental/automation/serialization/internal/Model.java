// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation.serialization.internal;

// Internally, model classes for YAML deserialization implement
// this interface so that they reference their associated public
// classes, so that "Find All References" on a public class will
// lead back to the model class.
interface Model<T> {
    T convert();
}
