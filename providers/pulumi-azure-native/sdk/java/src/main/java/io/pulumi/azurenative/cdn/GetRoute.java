// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.cdn;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.cdn.inputs.GetRouteArgs;
import io.pulumi.azurenative.cdn.outputs.GetRouteResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRoute {
    private GetRoute() {}
    /**
         * Friendly Routes name mapping to the any Routes or secret related information.
     * API Version: 2020-09-01.
     * 
     *
         * Friendly Routes name mapping to the any Routes or secret related information.
     * 
     */
    public static CompletableFuture<GetRouteResult> invokeAsync(GetRouteArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:cdn:getRoute", TypeShape.of(GetRouteResult.class), args == null ? GetRouteArgs.Empty : args, Utilities.withVersion(options));
    }
}