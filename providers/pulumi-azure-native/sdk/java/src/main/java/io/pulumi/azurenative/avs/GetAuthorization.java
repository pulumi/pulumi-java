// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.avs;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.avs.inputs.GetAuthorizationArgs;
import io.pulumi.azurenative.avs.outputs.GetAuthorizationResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAuthorization {
    private GetAuthorization() {}
    /**
         * ExpressRoute Circuit Authorization
     * API Version: 2020-03-20.
     * 
     *
         * ExpressRoute Circuit Authorization
     * 
     */
    public static CompletableFuture<GetAuthorizationResult> invokeAsync(GetAuthorizationArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:avs:getAuthorization", TypeShape.of(GetAuthorizationResult.class), args == null ? GetAuthorizationArgs.Empty : args, Utilities.withVersion(options));
    }
}