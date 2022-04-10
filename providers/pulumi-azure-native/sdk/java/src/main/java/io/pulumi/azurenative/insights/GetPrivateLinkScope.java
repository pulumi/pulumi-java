// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.insights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.insights.inputs.GetPrivateLinkScopeArgs;
import io.pulumi.azurenative.insights.outputs.GetPrivateLinkScopeResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetPrivateLinkScope {
    private GetPrivateLinkScope() {}
    /**
         * An Azure Monitor PrivateLinkScope definition.
     * API Version: 2019-10-17-preview.
     * 
     *
         * An Azure Monitor PrivateLinkScope definition.
     * 
     */
    public static CompletableFuture<GetPrivateLinkScopeResult> invokeAsync(GetPrivateLinkScopeArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:insights:getPrivateLinkScope", TypeShape.of(GetPrivateLinkScopeResult.class), args == null ? GetPrivateLinkScopeArgs.Empty : args, Utilities.withVersion(options));
    }
}