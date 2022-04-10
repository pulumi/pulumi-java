// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.costmanagement;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.costmanagement.inputs.GetViewByScopeArgs;
import io.pulumi.azurenative.costmanagement.outputs.GetViewByScopeResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetViewByScope {
    private GetViewByScope() {}
    /**
         * States and configurations of Cost Analysis.
     * API Version: 2019-11-01.
     * 
     *
         * States and configurations of Cost Analysis.
     * 
     */
    public static CompletableFuture<GetViewByScopeResult> invokeAsync(GetViewByScopeArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:costmanagement:getViewByScope", TypeShape.of(GetViewByScopeResult.class), args == null ? GetViewByScopeArgs.Empty : args, Utilities.withVersion(options));
    }
}