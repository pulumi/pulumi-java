// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.network;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.network.inputs.GetExpressRouteCrossConnectionPeeringArgs;
import io.pulumi.azurenative.network.outputs.GetExpressRouteCrossConnectionPeeringResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetExpressRouteCrossConnectionPeering {
    private GetExpressRouteCrossConnectionPeering() {}
    /**
         * Peering in an ExpressRoute Cross Connection resource.
     * API Version: 2020-11-01.
     * 
     *
         * Peering in an ExpressRoute Cross Connection resource.
     * 
     */
    public static CompletableFuture<GetExpressRouteCrossConnectionPeeringResult> invokeAsync(GetExpressRouteCrossConnectionPeeringArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:network:getExpressRouteCrossConnectionPeering", TypeShape.of(GetExpressRouteCrossConnectionPeeringResult.class), args == null ? GetExpressRouteCrossConnectionPeeringArgs.Empty : args, Utilities.withVersion(options));
    }
}