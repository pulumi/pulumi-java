// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.compute_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.compute_v1.inputs.GetVpnGatewayArgs;
import io.pulumi.googlenative.compute_v1.outputs.GetVpnGatewayResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetVpnGateway {
    private GetVpnGateway() {}
    /**
         * Returns the specified VPN gateway. Gets a list of available VPN gateways by making a list() request.
     * 
     */
    public static CompletableFuture<GetVpnGatewayResult> invokeAsync(GetVpnGatewayArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:compute/v1:getVpnGateway", TypeShape.of(GetVpnGatewayResult.class), args == null ? GetVpnGatewayArgs.Empty : args, Utilities.withVersion(options));
    }
}