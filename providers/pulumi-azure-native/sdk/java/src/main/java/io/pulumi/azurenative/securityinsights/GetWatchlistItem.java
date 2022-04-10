// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.securityinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.securityinsights.inputs.GetWatchlistItemArgs;
import io.pulumi.azurenative.securityinsights.outputs.GetWatchlistItemResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetWatchlistItem {
    private GetWatchlistItem() {}
    /**
         * Represents a Watchlist item in Azure Security Insights.
     * API Version: 2021-03-01-preview.
     * 
     *
         * Represents a Watchlist item in Azure Security Insights.
     * 
     */
    public static CompletableFuture<GetWatchlistItemResult> invokeAsync(GetWatchlistItemArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:securityinsights:getWatchlistItem", TypeShape.of(GetWatchlistItemResult.class), args == null ? GetWatchlistItemArgs.Empty : args, Utilities.withVersion(options));
    }
}