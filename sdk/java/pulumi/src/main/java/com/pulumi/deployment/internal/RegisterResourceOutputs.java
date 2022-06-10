package com.pulumi.deployment.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.Resource;

import java.util.Map;

@InternalUse
public interface RegisterResourceOutputs {
    void registerResourceOutputs(Resource resource, Output<Map<String, Output<?>>> outputs);
}
