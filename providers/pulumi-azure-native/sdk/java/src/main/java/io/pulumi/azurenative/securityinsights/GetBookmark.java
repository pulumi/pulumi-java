// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.securityinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.securityinsights.inputs.GetBookmarkArgs;
import io.pulumi.azurenative.securityinsights.outputs.GetBookmarkResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetBookmark {
    private GetBookmark() {}
    /**
         * Represents a bookmark in Azure Security Insights.
     * API Version: 2020-01-01.
     * 
     *
         * Represents a bookmark in Azure Security Insights.
     * 
     */
    public static CompletableFuture<GetBookmarkResult> invokeAsync(GetBookmarkArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:securityinsights:getBookmark", TypeShape.of(GetBookmarkResult.class), args == null ? GetBookmarkArgs.Empty : args, Utilities.withVersion(options));
    }
}