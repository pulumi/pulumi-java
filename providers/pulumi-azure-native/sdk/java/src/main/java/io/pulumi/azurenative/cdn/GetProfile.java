// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.cdn;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.cdn.inputs.GetProfileArgs;
import io.pulumi.azurenative.cdn.outputs.GetProfileResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetProfile {
    private GetProfile() {}
    /**
         * CDN profile is a logical grouping of endpoints that share the same settings, such as CDN provider and pricing tier.
     * API Version: 2020-09-01.
     * 
     *
         * CDN profile is a logical grouping of endpoints that share the same settings, such as CDN provider and pricing tier.
     * 
     */
    public static CompletableFuture<GetProfileResult> invokeAsync(GetProfileArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:cdn:getProfile", TypeShape.of(GetProfileResult.class), args == null ? GetProfileArgs.Empty : args, Utilities.withVersion(options));
    }
}