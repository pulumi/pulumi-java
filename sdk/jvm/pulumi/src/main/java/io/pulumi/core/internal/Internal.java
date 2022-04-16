package io.pulumi.core.internal;

import io.pulumi.asset.AssetOrArchive;
import io.pulumi.asset.AssetOrArchive.AssetOrArchiveInternal;
import io.pulumi.core.Output;
import io.pulumi.deployment.CallOptions;
import io.pulumi.deployment.CallOptions.CallOptionsInternal;
import io.pulumi.deployment.DeploymentInstance;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.deployment.InvokeOptions.InvokeOptionsInternal;
import io.pulumi.deployment.internal.DeploymentInstanceInternal;
import io.pulumi.resources.ComponentResource;
import io.pulumi.resources.ComponentResource.ComponentResourceInternal;
import io.pulumi.resources.CustomResource;
import io.pulumi.resources.CustomResource.CustomResourceInternal;
import io.pulumi.resources.InputArgs;
import io.pulumi.resources.InputArgs.InputArgsInternal;
import io.pulumi.resources.ProviderResource;
import io.pulumi.resources.ProviderResource.ProviderResourceInternal;
import io.pulumi.resources.Resource;
import io.pulumi.resources.Resource.ResourceInternal;
import io.pulumi.resources.Stack;
import io.pulumi.resources.Stack.StackInternal;

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

    public static StackInternal from(Stack s) {
        return StackInternal.from(s);
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
