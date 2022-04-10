// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.compute_alpha;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.compute_alpha.inputs.GetRegionInstanceGroupManagerArgs;
import io.pulumi.googlenative.compute_alpha.outputs.GetRegionInstanceGroupManagerResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRegionInstanceGroupManager {
    private GetRegionInstanceGroupManager() {}
    /**
         * Returns all of the details about the specified managed instance group.
     * 
     */
    public static CompletableFuture<GetRegionInstanceGroupManagerResult> invokeAsync(GetRegionInstanceGroupManagerArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:compute/alpha:getRegionInstanceGroupManager", TypeShape.of(GetRegionInstanceGroupManagerResult.class), args == null ? GetRegionInstanceGroupManagerArgs.Empty : args, Utilities.withVersion(options));
    }
}