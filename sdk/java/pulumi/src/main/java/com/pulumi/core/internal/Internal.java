package com.pulumi.core.internal;

import com.pulumi.asset.AssetOrArchive;
import com.pulumi.asset.AssetOrArchive.AssetOrArchiveInternal;
import com.pulumi.core.Output;
import com.pulumi.deployment.CallOptions;
import com.pulumi.deployment.CallOptions.CallOptionsInternal;
import com.pulumi.deployment.DeploymentInstance;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.deployment.InvokeOptions.InvokeOptionsInternal;
import com.pulumi.deployment.internal.DeploymentInstanceInternal;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResource.ComponentResourceInternal;
import com.pulumi.resources.CustomResource;
import com.pulumi.resources.CustomResource.CustomResourceInternal;
import com.pulumi.resources.InputArgs;
import com.pulumi.resources.InputArgs.InputArgsInternal;
import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.ProviderResource.ProviderResourceInternal;
import com.pulumi.resources.Resource;
import com.pulumi.resources.Resource.ResourceInternal;

public class Internal {

    private Internal() {
        throw new UnsupportedOperationException("static class");
    }

    public static <T> OutputInternal<T> of(Output<T> output) {
        return OutputInternal.cast(output);
    }

    public static DeploymentInstanceInternal of(DeploymentInstance deployment) {
        return DeploymentInstanceInternal.cast(deployment);
    }

    public static CallOptionsInternal from(CallOptions o) {
        return CallOptionsInternal.from(o);
    }

    public static InvokeOptionsInternal from(InvokeOptions o) {
        return InvokeOptionsInternal.from(o);
    }

    public static InputArgsInternal from(InputArgs a) {
        return InputArgsInternal.from(a);
    }

    public static ProviderResourceInternal from(ProviderResource r) {
        return ProviderResourceInternal.from(r);
    }

    public static CustomResourceInternal from(CustomResource r) {
        return CustomResourceInternal.from(r);
    }

    public static ComponentResourceInternal from(ComponentResource r) {
        return ComponentResourceInternal.from(r);
    }

    public static ResourceInternal from(Resource r) {
        return ResourceInternal.from(r);
    }

    public static AssetOrArchiveInternal from(AssetOrArchive a) {
        return AssetOrArchiveInternal.from(a);
    }
}
