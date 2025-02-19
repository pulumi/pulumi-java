package com.pulumi.provider.internal.testdata;

import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

public final class TestComponent extends ComponentResource {
    public TestComponent(String name, TestArgs args, ComponentResourceOptions options) {
        super("test-package:index:TestComponent", name, args, options);
    }
}
