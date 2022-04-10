// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.avs;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.avs.inputs.GetWorkloadNetworkSegmentArgs;
import io.pulumi.azurenative.avs.outputs.GetWorkloadNetworkSegmentResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetWorkloadNetworkSegment {
    private GetWorkloadNetworkSegment() {}
    /**
         * NSX Segment
     * API Version: 2020-07-17-preview.
     * 
     *
         * NSX Segment
     * 
     */
    public static CompletableFuture<GetWorkloadNetworkSegmentResult> invokeAsync(GetWorkloadNetworkSegmentArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:avs:getWorkloadNetworkSegment", TypeShape.of(GetWorkloadNetworkSegmentResult.class), args == null ? GetWorkloadNetworkSegmentArgs.Empty : args, Utilities.withVersion(options));
    }
}