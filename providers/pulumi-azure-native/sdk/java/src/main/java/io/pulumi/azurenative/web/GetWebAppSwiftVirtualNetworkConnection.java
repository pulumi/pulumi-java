// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.web;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.web.inputs.GetWebAppSwiftVirtualNetworkConnectionArgs;
import io.pulumi.azurenative.web.outputs.GetWebAppSwiftVirtualNetworkConnectionResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetWebAppSwiftVirtualNetworkConnection {
    private GetWebAppSwiftVirtualNetworkConnection() {}
    /**
         * Swift Virtual Network Contract. This is used to enable the new Swift way of doing virtual network integration.
     * API Version: 2020-10-01.
     * 
     *
         * Swift Virtual Network Contract. This is used to enable the new Swift way of doing virtual network integration.
     * 
     */
    public static CompletableFuture<GetWebAppSwiftVirtualNetworkConnectionResult> invokeAsync(GetWebAppSwiftVirtualNetworkConnectionArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:web:getWebAppSwiftVirtualNetworkConnection", TypeShape.of(GetWebAppSwiftVirtualNetworkConnectionResult.class), args == null ? GetWebAppSwiftVirtualNetworkConnectionArgs.Empty : args, Utilities.withVersion(options));
    }
}