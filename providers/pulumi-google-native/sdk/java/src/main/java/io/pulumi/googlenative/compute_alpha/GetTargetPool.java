// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.compute_alpha;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.compute_alpha.inputs.GetTargetPoolArgs;
import io.pulumi.googlenative.compute_alpha.outputs.GetTargetPoolResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetTargetPool {
    private GetTargetPool() {}
    /**
         * Returns the specified target pool. Gets a list of available target pools by making a list() request.
     * 
     */
    public static CompletableFuture<GetTargetPoolResult> invokeAsync(GetTargetPoolArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:compute/alpha:getTargetPool", TypeShape.of(GetTargetPoolResult.class), args == null ? GetTargetPoolArgs.Empty : args, Utilities.withVersion(options));
    }
}