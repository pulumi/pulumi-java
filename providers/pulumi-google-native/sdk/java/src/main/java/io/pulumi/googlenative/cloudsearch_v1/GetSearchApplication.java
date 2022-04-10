// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.cloudsearch_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.cloudsearch_v1.inputs.GetSearchApplicationArgs;
import io.pulumi.googlenative.cloudsearch_v1.outputs.GetSearchApplicationResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSearchApplication {
    private GetSearchApplication() {}
    /**
         * Gets the specified search application. **Note:** This API requires an admin account to execute.
     * 
     */
    public static CompletableFuture<GetSearchApplicationResult> invokeAsync(GetSearchApplicationArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:cloudsearch/v1:getSearchApplication", TypeShape.of(GetSearchApplicationResult.class), args == null ? GetSearchApplicationArgs.Empty : args, Utilities.withVersion(options));
    }
}