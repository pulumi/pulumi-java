package com.pulumi.provider.internal.testdata;

import com.pulumi.resources.ResourceArgs;
import com.pulumi.core.annotations.Import;

public final class TestArgs extends ResourceArgs {
    @Import(name="testProperty")
    private String testProperty;
}
