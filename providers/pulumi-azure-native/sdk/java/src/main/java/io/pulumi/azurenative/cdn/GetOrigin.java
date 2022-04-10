// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.cdn;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.cdn.inputs.GetOriginArgs;
import io.pulumi.azurenative.cdn.outputs.GetOriginResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetOrigin {
    private GetOrigin() {}
    /**
         * CDN origin is the source of the content being delivered via CDN. When the edge nodes represented by an endpoint do not have the requested content cached, they attempt to fetch it from one or more of the configured origins.
     * API Version: 2020-09-01.
     * 
     *
         * CDN origin is the source of the content being delivered via CDN. When the edge nodes represented by an endpoint do not have the requested content cached, they attempt to fetch it from one or more of the configured origins.
     * 
     */
    public static CompletableFuture<GetOriginResult> invokeAsync(GetOriginArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:cdn:getOrigin", TypeShape.of(GetOriginResult.class), args == null ? GetOriginArgs.Empty : args, Utilities.withVersion(options));
    }
}